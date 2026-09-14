# NERV Audit 2.1.0 release and upgrade notes

## Changes

- Vertical audit reads normalize supported PostgreSQL, Hibernate and JDBC timestamp
  values to `Instant`, with explicit UTC semantics for zone-free values.
- An overridable Spring `Clock` bean defaults to `Clock.systemUTC()` and supplies
  application-generated audit timestamps. External and Envers timestamps retain
  their own source of truth.
- Packaged PostgreSQL templates define fresh schemas and forward upgrades. A
  read-only startup validator checks vertical audit schema compatibility.
- The dependency-only starter supplies sources and Javadoc placeholder artifacts
  pointing to the implementation modules.

## Compatibility with 2.0.x

**This is not an unconditional drop-in upgrade.** Although the selected release
version is 2.1.0, default-on vertical schema validation changes startup behavior.
An application that previously started with an incompatible schema can now fail
with `VerticalAuditSchemaValidationException`. Documentation does not make that
behavior backward compatible; consumers following strict SemVer should treat this
as a breaking operational change.

Already compatible vertical schemas need no structural changes. Horizontal mode
and applications without audited entities skip this validation. The Java 21
baseline is unchanged.

## Upgrade procedure

1. Inventory entity and collection audit tables, revision mappings and sequences.
   Back up and rehearse migrations against a copy of the production database.
2. For zone-free PostgreSQL `updated` columns, establish the historical timezone
   before adapting V002. Its supplied conversion assumes UTC wall-clock values;
   do not use it unchanged for history recorded in another timezone.
3. Resolve null required values and orphan revisions without discarding history.
   Render V003 for each legacy table requiring the contract upgrade, after V002
   where needed. Adapt its default revision mapping for custom revision tables.
   Preserve existing migration checksums and revision sequence values.
4. Apply application-owned migrations before starting NERV Audit 2.1.0. Do not
   apply fresh-install V001 scripts over existing tables.
5. Start with validation enabled and verify audit writes, history and date filters.

The [persistence guide](vertical-audit-persistence.md) contains the schema contract,
SQL rendering instructions and timezone conversion details.

For a staged rollout, the existing explicit opt-out is:

```properties
nerv.audit.vertical.schema-validation.enabled=false
```

This only disables the startup check; it does not repair the schema or timestamp
compatibility. Re-enable validation after completing the migrations.

## Release verification

The publishing workflow verifies the checked-out release candidate with Java 21
and `mvn clean verify -Ppostgresql` before importing the signing key. Deployment
also enables the PostgreSQL profile. A failed verification prevents publication.

Local verification of this candidate passed 95 regular tests and 18 PostgreSQL
tests, without failures or skips. All five modules supply binary, sources and
Javadoc JARs, and all four PostgreSQL resources match their packaged copies.
The earlier showcase verification is recorded in the
[schema report](vertical-schema-verification.md); it used the then-current 2.0.1
coordinates and is not a new consumer verification of the 2.1.0 release artifacts.

Signing credentials and actual Maven Central validation must still be exercised
by the release environment; local verification does not establish their validity.
