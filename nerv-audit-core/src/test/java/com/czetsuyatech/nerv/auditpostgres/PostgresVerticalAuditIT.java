package com.czetsuyatech.nerv.auditpostgres;

import com.czetsuyatech.nerv.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.nerv.audit.persistence.VerticalAuditSchemaValidator;
import com.czetsuyatech.nerv.audit.persistence.VerticalAuditSchemaValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import com.czetsuyatech.nerv.audit.autoconfigure.NervAuditAutoConfiguration;
import com.czetsuyatech.nerv.audit.config.AuditConfig;
import com.czetsuyatech.nerv.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.nerv.audit.persistence.repository.AuditRepository;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PostgresVerticalAuditIT {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T12:34:56.123456Z"), ZoneOffset.UTC);
  private SessionFactory factory;
  private Connection connection;
  private String schema;

  @BeforeAll
  static void startPostgres() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopPostgres() {
    POSTGRES.stop();
  }

  @BeforeEach
  void migrateAndBuild() throws Exception {
    schema = "audit_" + UUID.randomUUID().toString().replace("-", "");
    connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    sql("create schema " + schema);
    sql("set search_path to " + schema);
    sql(resource("V001__nerv_audit_revision_schema.sql"));
    for (String table : List.of("sample_aud", "sample_tags_aud")) {
      sql(resource("V001__nerv_audit_vertical_table.sql.template").replace("${auditTable}", table));
    }
    sql("create table sample (id bigint primary key, name varchar(255), updated timestamptz)");
    sql("create table sample_tags (Sample_id bigint not null, tags varchar(255))");
    factory = new Configuration().addAnnotatedClass(Sample.class)
        .setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl())
        .setProperty("hibernate.connection.username", POSTGRES.getUsername())
        .setProperty("hibernate.connection.password", POSTGRES.getPassword())
        .setProperty("hibernate.default_schema", schema)
        .setProperty("hibernate.hbm2ddl.auto", "none")
        .buildSessionFactory();
    new NervEnversListenerConfigurer(factory, AuditConfig.builder().auditInsert(true).build(), CLOCK).afterPropertiesSet();
  }

  @AfterEach
  void close() throws Exception {
    if (factory != null) {
      factory.close();
    }
    if (connection != null) {
      sql("drop schema " + schema + " cascade");
      connection.close();
    }
  }

  @Test
  void suppliedTimestampsAndPostgresConversionRemainIndependentOfClock() {
    assertThat(java.util.TimeZone.getDefault().getID()).isEqualTo("Pacific/Honolulu");
    new VerticalAuditSchemaValidator().validate(factory);
    Instant first = Instant.parse("2026-01-15T10:20:30.123456Z");
    Instant second = first.plusSeconds(60);
    factory.inTransaction(session -> {
      Sample sample = new Sample();
      sample.id = 7L;
      sample.name = "before";
      sample.updated = first;
      sample.tags.add("first tag");
      session.persist(sample);
    });
    factory.inTransaction(session -> {
      Sample sample = session.find(Sample.class, 7L);
      sample.name = "after";
      sample.updated = second;
      sample.tags.add("second tag");
    });
    try (var session = factory.openSession()) {
      var repository = new AuditRepository(session, new AuditSqlBuilder(),
          entity -> Optional.of(schema + ".sample_aud"));
      var query = AuditQuery.builder().build();
      query.setEntities(List.of(Sample.class.getName()));
      query.setId(7L);
      query.setSortDirection("DESC");
      query.setFieldName("NAME");
      var history = repository.findAuditsByQuery(query);
      assertThat(history.getTotal()).isEqualTo(2);
      assertThat(history.getContent()).extracting(row -> row.getUpdated()).containsExactly(second, first);
      assertThat(history.getContent()).extracting(row -> row.getRevisionNo()).doesNotHaveDuplicates();
      query.setFromDate(second);
      query.setToDate(second);
      assertThat(repository.findAuditsByQuery(query).getContent()).hasSize(1);
    }
  }

  @Test
  void springClockControlsEntityCollectionAndMergedWorkUnitsAcrossJvmTimezones() {
    TimeZone original = TimeZone.getDefault();
    try {
      runner().withBean(Clock.class, () -> CLOCK).withPropertyValues("nerv.audit.audit-insert=true")
          .run(context -> {
            assertThat(context).hasNotFailed();
            long id = 10;
            for (String zone : List.of("UTC", "Asia/Manila", "Pacific/Honolulu")) {
              TimeZone.setDefault(TimeZone.getTimeZone(zone));
              final long entityId = id++;
              factory.inTransaction(session -> {
                Sample sample = new Sample();
                sample.id = entityId;
                sample.name = "initial";
                sample.tags.add("one");
                session.persist(sample);
                session.flush();
                sample.name = "insert merged with update";
                sample.tags.add("two");
                session.flush();
              });
              factory.inTransaction(session -> {
                Sample sample = session.find(Sample.class, entityId);
                sample.name = "first update";
                sample.tags.remove("one");
                session.flush();
                sample.name = "second update";
                sample.tags.add("three");
                session.flush();
              });
              factory.inTransaction(session -> session.remove(session.find(Sample.class, entityId)));
              var repository = context.getBean(AuditRepository.class);
              var query = AuditQuery.builder().entities(List.of(Sample.class.getName()))
                  .id(entityId).limit(100).build();
              var history = repository.findAuditsByQuery(query).getContent();
              assertThat(history).isNotEmpty().allSatisfy(row ->
                  assertThat(row.getUpdated()).isEqualTo(CLOCK.instant()));
              assertThat(history).extracting(row -> row.getRevisionType()).contains(0L, 1L, 2L);
            }
            try (var session = factory.openSession()) {
              var collectionRepository = new AuditRepository(session, new AuditSqlBuilder(),
                  entity -> Optional.of(schema + ".sample_tags_aud"));
              var collectionHistory = collectionRepository.findAuditsByQuery(
                  AuditQuery.builder().entities(List.of("tags")).limit(100).build()).getContent();
              assertThat(collectionHistory).isNotEmpty().allSatisfy(row ->
                  assertThat(row.getUpdated()).isEqualTo(CLOCK.instant()));
            }
          });
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void startupValidatesOfficialMigrationAndReportsMissingTable() throws Exception {
    runner().run(context -> assertThat(context).hasNotFailed());
    sql("drop table sample_aud");
    assertProblem("Missing table: " + schema + ".sample_aud");
    runner().run(context -> assertThat(context).hasFailed()
        .getFailure().isInstanceOf(VerticalAuditSchemaValidationException.class)
        .hasStackTraceContaining("Missing table: " + schema + ".sample_aud"));
  }

  @Test
  void checksCollectionTablesToo() throws Exception {
    sql("drop table sample_tags_aud");
    assertProblem("Missing table: " + schema + ".sample_tags_aud");
  }

  @Test
  void missingColumnHasActionableDiagnostic() throws Exception {
    sql("alter table sample_aud drop column updated_by");
    assertProblem("Missing column: " + schema + ".sample_aud.updated_by");
  }

  @Test
  void incompatibleTimestampAndExplicitUtcUpgrade() throws Exception {
    sql("alter table sample_aud alter column updated type timestamp without time zone using updated at time zone 'UTC'");
    sql("insert into revinfo values (99, 0)");
    sql("insert into sample_aud (id, rev, revtype, updated) values (7, 99, 0, '2026-01-15 10:20:30.123456')");
    assertProblem("expected timestamp with time zone but found timestamp");
    sql("set time zone 'Asia/Manila'");
    sql(resource("V002__nerv_audit_vertical_utc_upgrade.sql.template").replace("${auditTable}", "sample_aud"));
    new VerticalAuditSchemaValidator().validate(factory);
    try (var statement = connection.createStatement(); var rs = statement.executeQuery("select updated from sample_aud")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getObject(1, java.time.OffsetDateTime.class).toInstant())
          .isEqualTo(Instant.parse("2026-01-15T10:20:30.123456Z"));
    }
  }

  @Test
  void disabledValidationAndHorizontalModeSkipInvalidSchemaAtStartup() throws Exception {
    sql("drop table sample_aud");
    runner().withPropertyValues("nerv.audit.vertical.schema-validation.enabled=false")
        .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("verticalAuditSchemaValidation"));
    runner().withPropertyValues("nerv.audit.audit-strategy-type=HORIZONTAL")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void missingRevisionRelationshipAndSequenceFailClearly() throws Exception {
    sql("alter table sample_aud drop constraint sample_aud_rev_fkey");
    assertProblem("Missing foreign key: " + schema + ".sample_aud.rev");
    sql("drop sequence revinfo_seq");
    assertProblem("Missing revision sequence: " + schema + ".revinfo_seq");
  }

  @Test
  void performanceIndexesAreOptionalButRequiredNullabilityIsChecked() throws Exception {
    sql("drop index sample_aud_id_rev_idx, sample_aud_rev_idx, sample_aud_updated_idx");
    new VerticalAuditSchemaValidator().validate(factory);
    sql("alter table sample_aud alter column updated drop not null");
    assertProblem("sample_aud.updated must be NOT NULL");
  }

  @Test
  void revisionPrimaryKeyAndIncrementAreChecked() throws Exception {
    sql("alter sequence revinfo_seq increment by 1");
    assertProblem("expected increment 50 but found 1");
    sql("alter table revinfo drop constraint revinfo_pkey cascade");
    assertProblem("Missing primary key: " + schema + ".revinfo");
  }

  @Test
  void missingUpdatedAndWrongValueTypesAreChecked() throws Exception {
    sql("alter table sample_aud drop column updated");
    assertProblem("Missing column: " + schema + ".sample_aud.updated");
    sql("alter table sample_aud alter column new_value type bytea using null");
    assertProblem("sample_aud.new_value expected text/varchar but found bytea");
  }

  @Test
  void quotedResolverTargetsAreValidatedWithoutConfusingUnderscoreWildcards() throws Exception {
    sql(resource("V001__nerv_audit_vertical_table.sql.template").replace("${auditTable}", "\"Other_AUD\""));
    var resolver = (com.czetsuyatech.nerv.audit.persistence.AuditTableResolver)
        entity -> Optional.of(schema + ".\"Other_AUD\"");
    new VerticalAuditSchemaValidator().validate(factory, resolver);
    sql("alter table \"Other_AUD\" rename column updated to \"UPDATED\"");
    assertThatThrownBy(() -> new VerticalAuditSchemaValidator().validate(factory, resolver))
        .hasMessageContaining("Missing column: " + schema + ".Other_AUD.updated");
  }

  private ApplicationContextRunner runner() {
    return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(NervAuditAutoConfiguration.class))
        .withBean(EntityManagerFactory.class, () -> factory, definition -> definition.setDestroyMethodName(""))
        .withBean(EntityManager.class, () -> factory.createEntityManager());
  }

  private void assertProblem(String message) {
    assertThatThrownBy(() -> new VerticalAuditSchemaValidator().validate(factory))
        .isInstanceOf(VerticalAuditSchemaValidationException.class).hasMessageContaining(message);
  }

  private void sql(String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private String resource(String name) throws Exception {
    try (var stream = getClass().getResourceAsStream("/db/nerv-audit/postgresql/" + name)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Entity(name = "Sample")
  @Table(name = "sample")
  @Audited
  public static class Sample {
    @Id
    Long id;
    String name;
    @Column(name = "updated")
    Instant updated;
    @ElementCollection
    @CollectionTable(name = "sample_tags")
    List<String> tags = new ArrayList<>();
  }
}
