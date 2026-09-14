package com.czetsuyatech.nerv.audit.persistence;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;

/** Read-only validation of the physical tables used by vertical audit. */
public class VerticalAuditSchemaValidator {

  public void validate(EntityManagerFactory factory) {
    validate(factory, entity -> java.util.Optional.empty());
  }

  public void validate(EntityManagerFactory factory, AuditTableResolver resolver) {
    var sessionFactory = factory.unwrap(SessionFactoryImplementor.class);
    var envers = sessionFactory.getServiceRegistry().getService(EnversService.class);
    if (envers == null || !envers.getEntitiesConfigurations().hasAuditedEntities()) {
      return;
    }
    List<String> problems = new ArrayList<>();
    var config = envers.getConfig();
    // The public query contract currently uses these fixed column names.
    if (!"REV".equalsIgnoreCase(config.getRevisionFieldName())
        || !"REVTYPE".equalsIgnoreCase(config.getRevisionTypePropertyName())) {
      problems.add("Vertical queries require Envers revision columns REV and REVTYPE");
    }
    var revision = (AbstractEntityPersister) sessionFactory.getMappingMetamodel()
        .getEntityDescriptor(config.getRevisionInfo().getRevisionInfoEntityName());
    Set<String> auditTables = new HashSet<>();
    sessionFactory.getMappingMetamodel().forEachEntityDescriptor(persister -> {
      if (envers.getEntitiesConfigurations().isVersioned(persister.getEntityName())) {
        resolver.resolve(persister.getEntityName()).ifPresent(auditTables::add);
      }
      // Envers entity and middle/collection mappings share the originalId composite identifier.
      if (config.getOriginalIdPropertyName().equals(persister.getIdentifierPropertyName())
          && persister.getIdentifierType().isComponentType()) {
        auditTables.add(persister.getTableName());
      }
    });
    try (var session = sessionFactory.openSession()) {
      session.doWork(connection -> {
        DatabaseMetaData metadata = connection.getMetaData();
        boolean postgres = "PostgreSQL".equals(metadata.getDatabaseProductName());
        String defaultSchema = (String) sessionFactory.getProperties().get("hibernate.default_schema");
        Table revisionTable = table(metadata, connection, revision.getTableName(), defaultSchema);
        String revisionId = identifier(metadata, Identifier.toIdentifier(revision.getIdentifierColumnNames()[0]));
        String revisionTime = identifier(metadata, Identifier.toIdentifier(revision.getPropertyColumnNames(
            config.getRevisionInfo().getRevisionInfoTimestampData().getName())[0]));
        Map<String, Column> revisionColumns = columns(metadata, revisionTable, problems);
        require(revisionTable, revisionColumns, revisionId, Set.of(Types.INTEGER, Types.BIGINT), "integer", problems);
        // The existing DefaultRevisionEntity contract stores epoch milliseconds, not a SQL timestamp.
        require(revisionTable, revisionColumns, revisionTime, Set.of(Types.BIGINT), "bigint epoch milliseconds", problems);
        if (!revisionColumns.isEmpty()) {
          Set<String> keys = new HashSet<>();
          try (var rs = metadata.getPrimaryKeys(revisionTable.catalog(), revisionTable.schema(), revisionTable.name())) {
            while (rs.next()) {
              keys.add(metadataName(metadata, rs.getString("COLUMN_NAME")));
            }
          }
          if (!keys.equals(Set.of(revisionId))) {
            problems.add("Missing primary key: " + revisionTable + " (" + revisionId + ")");
          }
        }
        for (String name : auditTables.stream().sorted().toList()) {
          Table audit = table(metadata, connection, name, defaultSchema);
          validate(connection, audit, revisionTable, revisionId, postgres, problems);
        }
        if (postgres && revision.getGenerator() instanceof SequenceStyleGenerator generator) {
          var structure = generator.getDatabaseStructure();
          if (structure.isPhysicalSequence()) {
            Table sequence = table(metadata, connection, structure.getPhysicalName().render(), defaultSchema);
            try (var statement = connection.prepareStatement(
                "select increment_by from pg_catalog.pg_sequences where schemaname = ? and sequencename = ?")) {
              statement.setString(1, sequence.schema());
              statement.setString(2, sequence.name());
              try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                  problems.add("Missing revision sequence: " + sequence);
                } else if (rs.getLong(1) != structure.getIncrementSize()) {
                  problems.add("Revision sequence " + sequence + " expected increment " + structure.getIncrementSize()
                      + " but found " + rs.getLong(1));
                }
              }
            }
          }
        }
      });
    } catch (RuntimeException exception) {
      var failure = new VerticalAuditSchemaValidationException(List.of(
          "Could not inspect schema using the audit persistence connection (" + exception.getClass().getSimpleName()
              + "); check database access and metadata permissions"));
      failure.initCause(exception);
      throw failure;
    }
    if (!problems.isEmpty()) {
      throw new VerticalAuditSchemaValidationException(problems);
    }
  }

  private void validate(Connection connection, Table table, Table revision, String revisionId,
      boolean postgres, List<String> problems) throws SQLException {
    var metadata = connection.getMetaData();
    Map<String, Column> columns = columns(metadata, table, problems);
    for (String name : List.of("id", "rev", "revtype")) {
      require(table, columns, fold(metadata, name),
          name.equals("id") ? Set.of(Types.BIGINT)
              : name.equals("rev") ? Set.of(Types.INTEGER, Types.BIGINT)
              : Set.of(Types.SMALLINT, Types.INTEGER, Types.BIGINT),
          "integer" + (name.equals("id") ? " (bigint)" : ""), problems);
    }
    for (String name : List.of("field_name", "old_value", "new_value", "updated_by")) {
      require(table, columns, fold(metadata, name), Set.of(Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR,
          Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB), "text/varchar", problems);
    }
    String updated = fold(metadata, "updated");
    Column timestamp = columns.get(updated);
    // PG JDBC may report timestamptz as Types.TIMESTAMP; TYPE_NAME is authoritative here.
    if (postgres && timestamp != null) {
      if (!Set.of("timestamptz", "timestamp with time zone").contains(timestamp.typeName().toLowerCase(Locale.ROOT))) {
        problems.add("Column " + table + ".updated expected timestamp with time zone but found "
            + timestamp.typeName() + "; apply the explicit UTC upgrade migration");
      }
    } else {
      require(table, columns, updated, Set.of(Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE),
          "timestamp (zone-free values must contain UTC)", problems);
    }
    if (postgres && !columns.isEmpty()) {
      for (String required : List.of("id", "rev", "revtype", "updated")) {
        Column column = columns.get(required);
        if (column != null && column.nullability() == DatabaseMetaData.columnNullable) {
          problems.add("Column " + table + "." + required + " must be NOT NULL");
        }
      }
      if (!hasRevisionForeignKey(connection, table, revision, revisionId)) {
        problems.add("Missing foreign key: " + table + ".rev -> " + revision + "." + revisionId
            + " (requires a validated single-column relationship)");
      }
    }
    // No (id, rev, field_name) uniqueness: collection rows use id=0 and can repeat within a revision.
    // Secondary indexes affect performance only and are deliberately not startup requirements.
  }

  private static boolean hasRevisionForeignKey(Connection connection, Table table, Table revision, String revisionId)
      throws SQLException {
    // JDBC imported-key rows alone cannot distinguish a partial composite match or NOT VALID FK.
    String sql = """
        SELECT 1 FROM pg_catalog.pg_constraint fk
        JOIN pg_catalog.pg_attribute source ON source.attrelid = fk.conrelid AND source.attname = 'rev'
        JOIN pg_catalog.pg_attribute target ON target.attrelid = fk.confrelid AND target.attname = ?
        WHERE fk.contype = 'f' AND fk.convalidated
          AND fk.conrelid = to_regclass(?) AND fk.confrelid = to_regclass(?)
          AND fk.conkey = ARRAY[source.attnum] AND fk.confkey = ARRAY[target.attnum]
        """;
    try (var statement = connection.prepareStatement(sql)) {
      statement.setString(1, revisionId);
      statement.setString(2, quote(table.schema()) + "." + quote(table.name()));
      statement.setString(3, quote(revision.schema()) + "." + quote(revision.name()));
      try (var rs = statement.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static Map<String, Column> columns(DatabaseMetaData metadata, Table table, List<String> problems)
      throws SQLException {
    Map<String, Column> columns = new HashMap<>();
    // Escape LIKE wildcards in schema/table names (audit names commonly contain underscores).
    try (var rs = metadata.getColumns(table.catalog(), pattern(metadata, table.schema()),
        pattern(metadata, table.name()), null)) {
      while (rs.next()) {
        columns.put(metadataName(metadata, rs.getString("COLUMN_NAME")), new Column(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"), rs.getInt("NULLABLE")));
      }
    }
    if (columns.isEmpty()) {
      problems.add("Missing table: " + table);
    }
    return columns;
  }

  private static void require(Table table, Map<String, Column> columns, String name, Set<Integer> types,
      String expected, List<String> problems) {
    if (columns.isEmpty()) {
      return;
    }
    Column column = columns.get(name);
    if (column == null) {
      problems.add("Missing column: " + table + "." + name);
    } else if (!types.contains(column.type())) {
      problems.add("Column " + table + "." + name + " expected " + expected + " but found " + column.typeName());
    }
  }

  private static Table table(DatabaseMetaData metadata, Connection connection, String name, String defaultSchema)
      throws SQLException {
    var parsed = QualifiedNameParser.INSTANCE.parse(name);
    String schema = parsed.getSchemaName() == null
        ? (defaultSchema == null ? connection.getSchema() : identifier(metadata, Identifier.toIdentifier(defaultSchema)))
        : identifier(metadata, parsed.getSchemaName());
    String tableName = identifier(metadata, parsed.getObjectName());
    if ("PostgreSQL".equals(metadata.getDatabaseProductName()) && parsed.getSchemaName() == null) {
      // Respect the actual search_path, including tables outside current_schema().
      try (var statement = connection.prepareStatement("select n.nspname from pg_catalog.pg_class c "
          + "join pg_catalog.pg_namespace n on n.oid=c.relnamespace where c.oid=to_regclass(?)")) {
        statement.setString(1, defaultSchema == null ? name : quote(defaultSchema) + "." + quote(tableName));
        try (var rs = statement.executeQuery()) {
          if (rs.next()) {
            schema = rs.getString(1);
          }
        }
      }
    }
    if ("H2".equals(metadata.getDatabaseProductName())) {
      // H2's CASE_INSENSITIVE_IDENTIFIERS can preserve names while SQL ignores case.
      try (var rs = metadata.getTables(connection.getCatalog(), null, null, new String[] {"TABLE", "BASE TABLE"})) {
        while (rs.next()) {
          if (tableName.equalsIgnoreCase(rs.getString("TABLE_NAME"))
              && (schema == null || schema.equalsIgnoreCase(rs.getString("TABLE_SCHEM")))) {
            tableName = rs.getString("TABLE_NAME");
            schema = rs.getString("TABLE_SCHEM");
            break;
          }
        }
      }
    }
    return new Table(parsed.getCatalogName() == null ? connection.getCatalog()
        : identifier(metadata, parsed.getCatalogName()), schema, tableName);
  }

  private static String quote(String name) {
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  private static String identifier(DatabaseMetaData metadata, Identifier identifier) throws SQLException {
    return identifier.isQuoted() ? identifier.getText() : fold(metadata, identifier.getText());
  }

  private static String metadataName(DatabaseMetaData metadata, String name) throws SQLException {
    return "H2".equals(metadata.getDatabaseProductName()) ? fold(metadata, name) : name;
  }

  private static String fold(DatabaseMetaData metadata, String name) throws SQLException {
    if (metadata.storesUpperCaseIdentifiers()) {
      return name.toUpperCase(Locale.ROOT);
    }
    return metadata.storesLowerCaseIdentifiers() || "H2".equals(metadata.getDatabaseProductName())
        ? name.toLowerCase(Locale.ROOT) : name;
  }

  private static String pattern(DatabaseMetaData metadata, String value) throws SQLException {
    if (value == null) {
      return null;
    }
    String escape = metadata.getSearchStringEscape();
    return value.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
  }

  private record Column(int type, String typeName, int nullability) {
  }

  private record Table(String catalog, String schema, String name) {
    @Override
    public String toString() {
      return schema == null ? name : schema + "." + name;
    }
  }
}
