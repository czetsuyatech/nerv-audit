# Vertical audit persistence

Audit timestamps represent absolute instants and are normalized to UTC. Production deployments
use explicit database migrations plus NERV Audit startup validation, with
`spring.jpa.hibernate.ddl-auto=none`. Hibernate's Envers-generated horizontal DDL is not the
vertical schema; neither `update` nor Hibernate schema validation should manage vertical tables.

## Database and runtime support

The PostgreSQL reference schema targets PostgreSQL 16 and 17, Java 21, Spring Boot 4.1.0 and the
project's managed Hibernate version. PostgreSQL 16 Testcontainers tests follow the sibling
NERV Event convention (`postgres:16-alpine`); the showcase uses PostgreSQL 17.6. H2 remains an
isolated compatibility test database. Other JDBC databases receive portable metadata checks,
but these PostgreSQL scripts must not be applied to another dialect.

## Schema and migrations

Core packages tool-neutral resources under `db/nerv-audit/postgresql/`, outside Flyway's default
`db/migration` search path so merely depending on Core cannot apply SQL automatically:

- `V001__nerv_audit_revision_schema.sql`: apply once for a **fresh** default Envers installation.
  It creates `revinfo(rev integer PRIMARY KEY, revtstmp bigint)` and `revinfo_seq`, starting at
  1 with increment 50, matching the default sequence-backed Hibernate revision generator.
- `V001__nerv_audit_vertical_table.sql.template`: render and apply for **each** audited entity
  table and collection/middle table. Replace `${auditTable}` with the physical SQL identifier
  from the Hibernate mapping (including `@AuditTable`, naming strategy and schema), never user
  input. A `.template` is deliberately not auto-discovered as a Flyway migration.
- `V002__nerv_audit_vertical_utc_upgrade.sql.template`: explicit conversion of existing
  UTC-valued `timestamp without time zone` columns; see the upgrade procedure below.

There is no fixed list of application audit tables: NERV Audit does not own consumer entities.
For `@Table(name="payment")` the default is `payment_aud`; an audited collection table needs
its own vertical table too. All share the same shape:

| Column | PostgreSQL type | Meaning |
|---|---|---|
| audit_row_id | bigint identity, primary key | Storage-only row identity; not part of the public API |
| id | bigint, not null | Entity identifier; collection writes currently use 0 |
| rev | integer, not null, FK to revinfo.rev | Transaction revision |
| revtype | smallint, not null, check 0–2 | Envers ADD/MOD/DEL ordinal |
| field_name | varchar(255) | Uppercase changed property |
| old_value / new_value | text, nullable | Serialized values, including collection elements |
| updated_by | varchar(255), nullable | Audit actor |
| updated | timestamp with time zone, not null | Absolute audit instant |

Values are text, not JSON: queries compare them as strings. There is no FK from `id` to the
live entity: deletion history must survive deletion of that entity. There is deliberately no
unique `(id, rev, field_name)` constraint, since collection rows can share all three values.
An existing entity-only table may retain its suitable primary key; the synthetic row identity
is not required by library SQL or by the validator.

Indexes are `(id, rev)` for entity history, `(rev)` for revision filtering and FK lookups,
and `(updated)` for the default ordering and date range predicates. The table selected by the
resolver supplies entity type; there is no persisted `entity_name` column. Field/actor/value
indexes are workload-specific and not installed by default. Performance indexes are not fatal
validation requirements.

Example using a migration runner or `psql` (run from the repository root):

```sh
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f nerv-audit-core/src/main/resources/db/nerv-audit/postgresql/V001__nerv_audit_revision_schema.sql
sed 's/${auditTable}/payment_aud/g' \
  nerv-audit-core/src/main/resources/db/nerv-audit/postgresql/V001__nerv_audit_vertical_table.sql.template \
  > /tmp/payment_aud.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -1 -f /tmp/payment_aud.sql
```

Set the same schema/search path as Hibernate before running these commands. Copy rendered SQL
into your application's versioned Flyway/Liquibase migrations if desired; neither tool is a
mandatory runtime dependency. Apply revision DDL once, then table DDL once per table. For custom
revision entities/generators, use their actual mapping for revision table/sequence DDL. Vertical
writes retain the existing `DefaultRevisionEntity` contract, and vertical queries require the
revision column names `rev` and `revtype`. Revision timestamps use epoch milliseconds (`bigint`),
not PostgreSQL timestamp columns.

## Timestamp contract

`Instant` is canonical on native query reads, query date parameters, vertical writes and DTOs.
The shared converter accepts `Instant`, `OffsetDateTime`, `ZonedDateTime`, `LocalDateTime`,
`java.sql.Timestamp` and `java.util.Date`. Offset/zoned values retain their absolute instant;
`Timestamp` and `Date` use `toInstant()`. Zone-free `LocalDateTime` means **UTC**, never the JVM
zone. This also defines the legacy H2/zone-free column contract. SQL `Date`/`Time`, null required
timestamps, strings, numbers and arbitrary objects fail with type/context diagnostics; arbitrary
objects are not stringified. Known safe invalid numeric/date/time values appear in diagnostics.

PostgreSQL `updated` uses `timestamptz` because the value is an absolute moment. PostgreSQL may
display it in the session zone, but that does not alter the stored instant. The library binds
writes as Hibernate `INSTANT`, and normalizes actual JDBC/Hibernate result types without consumer
casts. No JVM or connection timezone override is needed for this column. Precision follows the
database (PostgreSQL microseconds); no promise of nanosecond storage is made. Collection rows
without entity audit metadata use the current instant, as before. Revision `revtstmp` is separately
recorded by Envers and need not equal an entity-provided `updated` instant.

## Current application time

NERV Audit provides an overridable `Clock` bean named `nervAuditClock`, defaulting to
`Clock.systemUTC()`, following NERV Event's time-handling convention. Any application `Clock`
bean replaces the default by type, so Audit and Event can share the same configured clock.
For deterministic tests, provide:

```java
@Bean
Clock applicationClock() {
  return Clock.fixed(Instant.parse("2030-01-01T12:34:56Z"), ZoneOffset.UTC);
}
```

The clock is passed explicitly through the Envers listener configurer, entity/collection
listeners and work units, including merged work units. NERV-generated fallback `updated`
values use `Instant.now(clock)` when no entity timestamp is supplied. The clock's zone and the
JVM default zone do not change the resulting instant.

The clock does not replace supplied entity timestamps, JDBC/PostgreSQL timestamps, or Envers'
own revision timestamp. `AuditTimestampConverter` remains independent of Clock and retains its
explicit UTC conversion contract. Existing infrastructure constructors remain available for
manual integrations with a UTC system-clock default; use their Clock-taking overloads when
constructing these components outside Spring. No Clock-specific API was added to `nerv-audit-api`.

## Startup validation

Explicit auto-configuration runs a dedicated, read-only `VerticalAuditSchemaValidator` after
singleton/database initialization, with the initialized EntityManagerFactory and its connection.
Validation runs only for VERTICAL strategy with audited entities. HORIZONTAL mode skips it.
Excluding NERV Audit auto-configuration also excludes its lifecycle. Disable it explicitly with:

```properties
nerv.audit.vertical.schema-validation.enabled=false
```

It is **enabled by default** for production safety. It checks the mapped entity and collection
audit tables, additional query-resolver targets, required columns, compatible JDBC types,
PostgreSQL `timestamptz` and required nullability, the revision primary key, PostgreSQL revision foreign keys and the
actual mapped revision sequence/increment where sequence generation is used. It does not modify
anything. Missing schema or incompatible types throw `VerticalAuditSchemaValidationException`
with an immutable `getProblems()` list and actionable NERV Audit diagnostics. It intentionally
does not require optional performance indexes or the template's storage-only identity column.

Migrations must finish before the EntityManagerFactory/startup validation lifecycle. Standard
Spring Boot Flyway/Liquibase ordering provides this; custom migration runners must arrange the
same dependency. Validation uses application database permissions, so those must permit metadata
inspection. Runtime schema validation is a compatibility check, not a replacement for migration
checksums or a full database integrity audit.

## Existing installations and future upgrades

Default-on validation is a startup compatibility change. Inventory every entity and collection
table before rollout. Existing PostgreSQL installations with zone-free `updated` columns must
apply the V002 template. **First verify the historical timezone**: the old reader used the JVM
zone and old writes could depend on entity type/JDBC settings. The library cannot infer how an
existing deployment wrote its history. The supplied upgrade preserves known UTC wall-clock
values via `USING updated AT TIME ZONE 'UTC'`, independent of the session timezone. It rejects a
column already typed as `timestamptz`. If historical values denote another known zone, adapt the
conversion to that zone; mixed-zone or DST-ambiguous history requires an explicit data repair.
Back up, rehearse on a copy, and schedule the table lock/rewrite before application rollout.

Existing PostgreSQL tables also need `NOT NULL` on `id`, `rev`, `revtype` and `updated`, plus
valid `rev` foreign keys to the mapped revision table. Inspect/fix
orphan revisions and missing required values before adding constraints; do not drop history automatically.
For a legacy `payment_aud` table lacking those constraints, after repairing any invalid rows:

```sql
ALTER TABLE payment_aud
    ALTER COLUMN id SET NOT NULL,
    ALTER COLUMN rev SET NOT NULL,
    ALTER COLUMN revtype SET NOT NULL,
    ALTER COLUMN updated SET NOT NULL;
ALTER TABLE payment_aud ADD CONSTRAINT payment_aud_rev_fk
    FOREIGN KEY (rev) REFERENCES revinfo (rev);
```

Do not add a duplicate FK where one already exists. Existing sequence
names, increments and values must agree with the actual Hibernate mapping. Do not recreate or
reset existing revision sequences. Do not apply fresh-install V001 over existing tables.

Versioned SQL filenames define the schema contract; no NERV version table or migration engine
is introduced. Structural validation checks compatibility even for manually managed installations.
Future incompatible versions must ship explicit upgrade SQL and matching validator/tests. Track
applied versions in your deployment migrations, preserve checksums, and apply upgrades before
starting the corresponding library. To retire the showcase's old V11 workaround, its forward
V12 migration restores `timestamptz`; immutable V11 remains solely for migration history.

## Verification

```sh
mvn clean verify
mvn clean verify -Ppostgresql
```

The explicit PostgreSQL profile requires Docker and fails if it is unavailable (no silent skip).
Its isolated Failsafe JVM runs in `Pacific/Honolulu`. It applies packaged migration templates,
creates and revises an audited entity/collection, checks history and date filters as instants,
and exercises valid/invalid schema, startup opt-out, HORIZONTAL opt-out and legacy UTC upgrades.

The implementation and measured results are recorded in the [verification report](vertical-audit-verification.md).
