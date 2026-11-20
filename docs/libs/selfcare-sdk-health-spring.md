# selfcare-sdk-health-spring

> Spring Boot Actuator readiness indicators for SelfCare Spring services, probing downstream dependencies such as MongoDB and Azure Blob Storage.

| | |
|---|---|
| Path | `libs/selfcare-sdk-health-spring` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-health-spring:0.1.0` |
| Stack | Spring Boot Actuator 3.5.6 (`provided`), Java 17; parent [`selfcare-sdk-pom`](selfcare-sdk-pom.md) |
| Used by | `dashboard-bff`, `institution-ms` (root, `core`, `connector/dao`, `connector/azure-storage`), `user-group-ms` |

## What it provides

Package `it.pagopa.selfcare.commons.health.spring`:

| Class | Purpose |
|---|---|
| `AbstractReadinessIndicator` | Base `HealthIndicator` with timeout and stable `latencyMs` / `error` details. |
| `AbstractMongoReadinessIndicator` | Mongo base (`databaseName()`, optional `host()`, `hostFromConnectionString(...)`). |
| `AbstractBlobStorageReadinessIndicator` | Azure Blob Storage base. |
| `HealthIndicatorConstants` | Detail keys and default timeout (2 s). |

Independent from the Quarkus module [`selfcare-sdk-health`](selfcare-sdk-health.md).

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-health-spring</artifactId>
  <version>0.1.0</version>
</dependency>
<!-- plus org.springframework.boot:spring-boot-starter-actuator in the app -->
```

Implement an indicator as a Spring bean and add it to the readiness group, e.g.
`management.endpoint.health.group.readiness.include=readinessState,mongoReadiness`
(example in `libs/selfcare-sdk-health-spring/README.md`).

## Build and release

```bash
mvn -B -ntp -f libs/selfcare-sdk-health-spring/pom.xml clean verify
```

- PR: `.github/workflows/pr_health_sdk_spring.yml` (PR to `main` touching the lib or `libs/selfcare-sdk-pom/**`).
- Release: `.github/workflows/release_health_sdk_spring.yml`, push on `main` touching `libs/selfcare-sdk-health-spring/**`
  or `workflow_dispatch`; `clean verify` then `deploy -DskipTests` to `https://maven.pkg.github.com/pagopa/selfcare`.

## References

- [`libs/selfcare-sdk-health-spring/README.md`](../../libs/selfcare-sdk-health-spring/README.md)
- [selfcare-sdk-health](selfcare-sdk-health.md)
