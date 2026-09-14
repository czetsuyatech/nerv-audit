# Vertical audit schema contract — implementation and verification

Verified on 2026-09-14. Java 21, Spring Boot 4.1.0 and the existing NERV Audit 2.0.1 coordinates
remain unchanged. No artifacts were published. The completed Clock and timestamp converter
implementations were preserved.

## Existing Schema

The schema was derived from `AuditRepository`, `AuditSqlBuilder`, the entity and collection work
units, physical Envers mappings, default revision mapping, existing Liquibase test fixtures and
showcase Flyway DDL. There is no global vertical-audit entity table: every audited entity and
collection/middle mapping has its own physical audit table.

The writer inserts `id`, `rev`, `revtype`, `field_name`, `old_value`, `new_value`, `updated_by`,
and `updated`. The reader selects the same fields; its `entity_name` is a SQL literal derived
from the selected table, not another stored column. IDs are bigint-compatible; collection writes
currently use id 0. Values are serialized text, not JSON. A delete row permits null old/new values.
`revinfo.rev` identifies the revision and `revinfo.revtstmp` holds epoch milliseconds. PostgreSQL's
default revision generator uses `revinfo_seq` with increment 50; the validator inspects the actual
Hibernate generator rather than assuming that increment for every custom mapping.

Legacy H2 fixtures have nullable columns, bounded varchar values and a zone-free timestamp;
PostgreSQL showcase V10 already has required nullability, a revision FK and text values. Its
entity-only `(id, rev, field_name)` primary key is suitable there, but is not a universal collection
key. There must be no FK to the live entity, since deletion history must survive entity deletion.

## Official Migration

Resources are packaged under `nerv-audit-core/src/main/resources/db/nerv-audit/postgresql/`,
outside automatic Flyway discovery:

| Resource | Purpose / objects |
|---|---|
| Existing `V001__nerv_audit_revision_schema.sql` | Fresh `revinfo(rev integer PRIMARY KEY, revtstmp bigint)` and `revinfo_seq`, increment 50 |
| Existing `V001__nerv_audit_vertical_table.sql.template` | Each physical audit table: storage-only bigint identity PK, eight writer columns, revision FK, required nullability, defensive revision-type range check, and history indexes |
| Existing `V002__nerv_audit_vertical_utc_upgrade.sql.template` | Reviewed conversion of existing UTC-valued zone-free `updated` columns to timestamptz |
| **New `V003__nerv_audit_vertical_contract_upgrade.sql.template`** | Adopt required nullability, add/validate the single-column revision FK, and add missing recommended indexes without replacing existing rows, keys or sequences |

Consumers render `${auditTable}` using a trusted physical identifier, assign their own migration
versions and explicitly apply the SQL. No Flyway/Liquibase runtime dependency was added. Templates
are necessary because consumer entity and collection table names are not owned by the library.

Indexes `(id, rev)`, `(rev)` and `(updated)` follow entity-history, revision, default ordering and
time-range queries. These are performance recommendations; their absence is nonfatal. V003 reuses
compatible existing btree prefixes, including an existing entity-only primary key, so adoption
does not add redundant indexes. Actor/field/value indexes are left to measured workloads.

## Timestamp Schema

`updated` remains PostgreSQL `timestamp with time zone` (`timestamptz`) for absolute instants.
`revinfo.revtstmp` remains bigint epoch milliseconds. No new timestamp interpretation was introduced.
The shared Clock still generates fallback application time and the converter still normalizes
supplied/JDBC timestamps independently. V003 refuses zone-free timestamps and directs operators
to the reviewed V002 conversion instead of reinterpreting historical values automatically.

## Schema Validator

`VerticalAuditSchemaValidator` remains in Core's persistence package, separate from repositories,
work units and controllers. Explicit auto-configuration invokes it after database/singleton
initialization for VERTICAL strategy with audited mappings. It skips HORIZONTAL applications,
applications without audited entities, and explicit validation opt-out.

It checks entity/collection/resolver-target tables, required columns and compatible types,
PostgreSQL timestamptz/nullability, revision PK, revision relationship and mapped sequence/increment.
This change closes two PostgreSQL FK-validation gaps: `NOT VALID` constraints and one matching
component inside a composite FK no longer count as a valid single-column revision relationship.

Failures use `VerticalAuditSchemaValidationException` with NERV Audit context and an immutable
`getProblems()` list. Performance indexes and the storage-only audit-row identity are nonfatal.
A PostgreSQL read-only-transaction integration test verifies that validation works without DDL or
DML; an invalid schema produces diagnostics rather than repair.

## Configuration

No new property was necessary. Existing
`nerv.audit.vertical.schema-validation.enabled=true` remains the default, favoring fail-fast
production startup. Set it to `false` to skip validation explicitly. Clock configuration and
all existing public API contracts are unchanged.

## Migration/Validator Contract

PASS: every PostgreSQL test starts in an empty, isolated schema inside a real Testcontainers
PostgreSQL database and applies the packaged V001 revision/table resources. The validator accepts
that schema. CI already runs `mvn --batch-mode clean verify -Ppostgresql`, preserving this invariant.

V002 + V003 also upgrade a legacy table successfully. Tests verify exact history values/instants,
no duplicate index/constraint creation on re-adoption, and atomic failure on orphan history without
discarding the offending rows. Future changes must use new versioned SQL files; no custom schema
history/version table or migration framework was introduced.

## Existing Installation Compatibility

Already compatible V001-shaped schemas need no structural upgrade. Existing default-mapping
vertical tables missing nullability or a validated revision FK can adopt V003. It explicitly
validates an existing unvalidated FK and fails if historical rows are invalid. Repair orphan/null
data first; the migration does not delete it. V003 can also add recommended performance indexes
to the already-compatible showcase schema while retaining its existing primary key.

Existing zone-free timestamps require V002 **only after confirming historical values mean UTC**.
Other/mixed historical zones require reviewed conversion or data repair. Do not apply fresh V001
DDL over existing objects or reset revision sequences. Custom revision mappings require adapting
the migration to their actual table/column names. The strengthened validator may now reject a
previously accepted unvalidated/composite relationship; this is an explicit compatibility change.

## Vertical Audit Verification

PASS: after official migrations, tests create audited entities and collections, create multiple
revisions, query history/date filters, and verify revision numbers/types, field names, old/new
values, actors and exact instants. The fixed-Clock tests still cover entity/collection changes,
work-unit merges and deletes across multiple JVM default zones. Timestamp conversion code was
not changed for schema work.

## Showcase

Added `V13__adopt_nerv_audit_schema.sql`, exactly rendered from official V003 for `payment_aud`.
Added `AuditMigrationContractTest` and a test-scoped Core dependency to compare the committed
consumer migration with the packaged resource byte-for-byte after table-name substitution.
Updated auditing documentation to remove obsolete instructions to force JVM/session UTC and
store a zone-free column. V10–V12 remain immutable for Flyway checksum compatibility; no business
schema was moved into Audit. The supported contract now comes from the library migration.

The full showcase started after applying V13, then passed its PostgreSQL 17.6/Kafka acceptance
suite, including non-UTC audit HTTP timestamps compared against database instants.

## Tests

| Command | Result |
|---|---|
| Audit: `mvn clean verify` | PASS — 95 regular tests, zero failures/errors/skips |
| Audit: `mvn clean verify -Ppostgresql` | PASS — 95 regular + 18 PostgreSQL tests, zero failures/errors/skips |
| Showcase: `mvn clean verify -Pintegration-tests` with isolated Maven cache | PASS — 20 regular + 9 PostgreSQL/Kafka acceptance tests, zero failures/errors/skips |

PostgreSQL uses the established `postgres:16-alpine` Testcontainers image and a non-UTC Failsafe JVM;
the showcase uses `postgres:17.6`. The showcase command used
`-Dmaven.repo.local=/tmp/nerv-audit-schema-m2.5pW0ND/repository`, containing this locally built Audit
and published NERV Event 2.0.0 artifacts, preserving the independently modified Event artifacts in
the shared Maven cache. Local Maven installation into that isolated cache is not publication.

## Blockers

None for this task. Running against the independently modified Event build in the shared Maven
cache still requires that build's Event migration; verification above used the showcase's pinned,
published Event dependencies instead. No unrelated Event or business-schema changes were made.
