# NERV Audit

NERV Audit is an open-source Java 21 audit-trail library for Spring Boot applications. It has a
stable API boundary, a Hibernate Envers implementation, a transport-independent operations API, and
an optional HTTP management surface.

## Modules

- `nerv-audit-api` contains public contracts, DTOs, query models, enums, and extension points.
- `nerv-audit-core` implements the contracts with Hibernate Envers listeners, repositories, query
  builders, and audit services.
- `nerv-audit-operations` provides transport-independent, read-only audit search and history
  operations by delegating to Core.
- `nerv-audit-operations-web` provides optional Spring MVC management endpoints over Operations.
- `nerv-audit-spring-boot-starter` aggregates the four modules and is the recommended dependency
  for Spring Boot applications.

## Requirements

- Java 21
- Maven 3.9 or newer
- Spring Boot 4.x and Hibernate Envers in the consuming application

## Installation

Add the starter. It brings in Core, Operations, and Operations Web transitively and requires no
manual assembly, license, activation, additional repository, or credentials.

```xml
<dependency>
  <groupId>com.czetsuyatech.nerv</groupId>
  <artifactId>nerv-audit-spring-boot-starter</artifactId>
  <version>2.1.0</version>
</dependency>
```

## Basic configuration

Upgrading from 2.0.x? Read the [2.1.0 release and upgrade notes](docs/release-2.1.0.md)
before deployment. Default-on vertical schema validation can reject an existing schema at startup.

```yaml
nerv:
  audit:
    audit-strategy-type: VERTICAL # VERTICAL (default) or HORIZONTAL
    audit-insert: false
    audit-fields: createdBy,created,updatedBy,updated,originalId,revisionType,version
    operations:
      web:
        enabled: true
        base-path: /management/nerv-audit
```

Entities must be Envers-versioned, for example with `@Audited`. Define an `AuditTableResolver` bean
to override the default `{entity-table}_AUD` table resolution.

## Usage

With `nerv.audit.operations.web.enabled=true`, the starter exposes its opt-in operations surface:

```text
GET /management/nerv-audit/audits/vertical
GET /management/nerv-audit/audits/vertical/{entity}
GET /management/nerv-audit/audits/horizontal/{entity}
```

Vertical audit queries support filters such as `id`, `revisionNo`, `updatedBy`, `fieldName`,
`newValue`, `oldValue`, `fromDate`, `toDate`, `sortBy`, `sortDirection`, `page`, and `size`.

```text
GET /management/nerv-audit/audits/vertical/UserEntity?id=101&updatedBy=admin&page=0&size=20
```

For advanced integrations, depend directly on the individual module that owns the needed boundary.
Normal Spring Boot applications should depend only on the starter.

## Vertical audit persistence

Use the [PostgreSQL migration and validation guide](docs/vertical-audit-persistence.md) for
production schema setup, UTC timestamp semantics, existing-schema upgrades and default-on
startup validation. Production vertical audit uses explicit migrations with `ddl-auto=none`.

## Build and test

Run the complete build from the repository root:

```bash
mvn clean verify
# Real PostgreSQL integration coverage (Docker required):
mvn clean verify -Ppostgresql
```

To install the artifacts for local development:

```bash
mvn clean install
```

## License and issues

NERV Audit is licensed under the [Apache License 2.0](LICENSE.md). Report bugs or feature requests
through the repository's GitHub issues page.
