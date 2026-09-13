# Vertical audit integration fixes — verification report

Verified on 2026-09-13 with Java 21. Spring Boot 4.1.0, Hibernate 7.4.1.Final and
NERV Audit 2.0.1 coordinates remain unchanged. These fixes are local source/build changes;
the already-published 2.0.1 release does not acquire them automatically.

## Timestamp Root Cause

The showcase's PostgreSQL `timestamptz` query returned `java.time.Instant` through Hibernate.
`AuditRepository.map` unconditionally cast the fifth native result-array element to
`LocalDateTime`, causing `Instant cannot be cast to LocalDateTime`. Even when the cast worked,
it interpreted the value with the JVM default timezone, changing the absolute instant.

The path inventory found one vertical native-result mapper and one shared vertical write path
(`NervAuditWorkUnit`, including collection work units). The SQL builder already accepts `Instant`
range parameters, and vertical DTOs already expose `Instant`. Envers revision timestamps are
epoch-millisecond longs; the separate horizontal reader uses `Date.toInstant()` and was unchanged.

## Timestamp Fix

`AuditTimestampConverter` centralizes deterministic conversion for vertical reads and writes:
`Instant`, `OffsetDateTime`, `ZonedDateTime`, `LocalDateTime`, `java.sql.Timestamp`, and
`java.util.Date`. Canonical output is `Instant`; zone-free `LocalDateTime` explicitly means UTC.
SQL date-only/time-only values, null required values, numbers, strings and arbitrary objects fail
with clear type/context diagnostics. Safe invalid values are included; arbitrary objects are not
stringified. Vertical writes bind Hibernate `INSTANT`; no JVM default timezone is consulted.

The native writer also synchronizes the revision query space before inserting vertical rows.
Real PostgreSQL tests exposed a queued, sequence-backed revision insert under native Hibernate
bootstrapping; synchronization makes the revision FK valid without weakening the constraint.
The writer, default resolver and validator now use physical Hibernate audit mappings, including
collection tables, instead of independently reconstructing table names.

## PostgreSQL Verification

All **11** PostgreSQL integration tests passed using Testcontainers 2.0.5 and
`postgres:16-alpine` (server 16.15). The isolated Failsafe JVM uses `Pacific/Honolulu`.
Coverage includes audited entity/collection persistence, multiple revisions, exact timestamp
instants and date filtering, official migration validation, missing tables/columns, incompatible
timestamp/text types, missing FK/PK/sequence, incorrect sequence increment, required nullability,
optional indexes, quoted resolver targets, validation opt-out, HORIZONTAL opt-out and an explicit
UTC upgrade under a different database session timezone.

The full showcase additionally passed PostgreSQL **17.6** acceptance coverage. Its services run
in a non-UTC JVM and the audit HTTP endpoint's returned instants are compared directly with
PostgreSQL `OffsetDateTime.toInstant()` values.

## Showcase Workaround

Removed `TimeZone.setDefault(UTC)` from `PaymentApplication`, Hikari's `SET TIME ZONE 'UTC'`
connection initializer and the Hibernate JDBC timezone override that supported the workaround.
Normal NERV Audit services remain in use; there is no consumer timestamp-normalization layer.

Forward Flyway migration `V12__restore_audit_instant.sql` restores `payment_aud.updated` to
`timestamptz` using `AT TIME ZONE 'UTC'`. V11 is retained as immutable migration history to
preserve checksums for existing installations. After V12 its zone-free workaround is inactive.
Pre-existing unrelated showcase edits were preserved.

## Vertical Schema

Official, tool-neutral SQL resources are packaged under **`db/nerv-audit/postgresql/`**, outside
Flyway's default search path. No runtime migration tool or automatic DDL was introduced.

The fresh revision script creates `revinfo(rev integer PRIMARY KEY, revtstmp bigint)` and
`revinfo_seq` with increment 50, matching the default Hibernate mapping. A per-table template
creates each entity or collection audit table with:

- `audit_row_id`: bigint identity primary key, storage-only;
- `id`: bigint, `rev`: integer FK to `revinfo.rev`, `revtype`: smallint checked between 0 and 2;
- `field_name` and `updated_by`: varchar(255);
- `old_value` and `new_value`: nullable text;
- `updated`: non-null timestamp with time zone.

`id`, `rev` and `revtype` are also non-null. Indexes cover `(id, rev)`, `(rev)` and `(updated)`
for the actual history, revision and time-range queries. There is no FK to a live entity and no
universal unique `(id, rev, field_name)` constraint: collection rows use id 0 and may repeat
within a revision. Existing entity-only primary keys remain compatible; the synthetic row
identity and secondary indexes are not required by library queries.

## Timestamp Schema Decision

`updated` uses PostgreSQL **timestamp with time zone** because audit timestamps represent
absolute moments. Database display timezone does not change the stored instant. `revinfo.revtstmp`
remains an epoch-millisecond `bigint`, as mapped by Envers. Legacy zone-free values are accepted
by Java conversion as UTC, but PostgreSQL startup validation requires migrating the column to
`timestamptz` so future storage is unambiguous.

## Schema Validation

Explicit Spring Boot auto-configuration runs a dedicated, read-only validator after singleton
and database initialization, only for VERTICAL strategy with audited entities. It uses the
EntityManagerFactory's connection and physical mappings, also checking custom resolver targets.

Validation is **default-on**. It checks required tables/columns/types/nullability, PostgreSQL
`timestamptz`, the revision primary key, PostgreSQL revision foreign keys and mapped revision
sequences/increments. Optional indexes are nonfatal. Errors carry immutable structured problems
and clear NERV Audit diagnostics. Configure
`nerv.audit.vertical.schema-validation.enabled=false` to skip it explicitly. HORIZONTAL mode
also skips it. No schema mutation is performed.

## Schema Versioning

Versioned filenames plus structural compatibility checks were chosen. There is no new version
marker table or migration framework. Consumers track applied SQL in their deployment migrations;
future incompatible changes must ship explicit upgrade scripts and matching validator/tests.
CI now runs the PostgreSQL profile so the packaged migration and validator are verified together.

## Backward Compatibility

Public audit APIs and dependency versions remain unchanged. **Default-on validation and the
PostgreSQL timestamp requirement are startup/schema compatibility changes.** Existing zone-free
installations must apply the supplied V002 UTC upgrade template after verifying that historical
values really represent UTC. The script uses an explicit timezone and rejects an already-zoned
column. Historical non-UTC or mixed-zone data needs a reviewed conversion/repair, not a blind cast.

Legacy PostgreSQL tables also need the required NOT NULL constraints and revision FKs. Inspect
and repair null/orphan data before adding constraints. Existing revision sequences must match
the actual mapping; do not reset their values or apply fresh-install V001 over an existing schema.
The persistence guide includes application steps and legacy constraint SQL.

## Test Results

| Command / environment | Result |
|---|---|
| `mvn clean verify` — Audit reactor, Java 21 | PASS: 93 tests, no failures/errors/skips |
| `mvn clean verify -Ppostgresql` — Audit reactor | PASS before final test expansion |
| `mvn clean install -Ppostgresql` — final Audit build | PASS: 93 regular tests + 11 PostgreSQL tests, no failures/errors/skips; fixed artifacts installed locally |
| `mvn clean verify -Pintegration-tests` — showcase, shared Maven cache | Blocked by unrelated locally modified NERV Event 2.0.0 expecting absent `ordering_key` in `order-service` |
| Same showcase command with isolated Maven cache | PASS: 19 unit tests + 9 PostgreSQL/Kafka acceptance tests, no failures/errors/skips |

The isolated cache was cloned from the existing cache, retaining the locally fixed Audit build,
then only NERV Event artifacts were replaced by the published, unchanged 2.0.0 dependencies.
This preserved ongoing Event work and avoided unrelated showcase schema changes. Exact override:
`-Dmaven.repo.local=/tmp/nerv-audit-verification-m2.2Y4vNI/repository`.

An initial packaging test exposed Flyway auto-discovering the new revision SQL under its default
path. Moving resources to `db/nerv-audit/postgresql/` fixed that conflict; the final full showcase
run passed with ordinary Flyway configuration.

## Blockers

No remaining blocker for the Audit fixes or verification against the showcase's published
non-Audit dependencies. The shared local Maven cache still contains the independent NERV Event
schema change; running the showcase against that modified Event build requires its corresponding
Event migration. The isolated-cache verification above passes without changing that ongoing work.
