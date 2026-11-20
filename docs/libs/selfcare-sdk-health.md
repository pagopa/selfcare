# selfcare-sdk-health

> Reactive SmallRye/MicroProfile Health base classes for Quarkus readiness checks that actually exercise downstream dependencies (MongoDB, Azure Blob Storage, ...).

| | |
|---|---|
| Path | `libs/selfcare-sdk-health` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-health:0.2.0` |
| Stack | MicroProfile Health API, SmallRye Health API, Mutiny, Java 17 (Quarkus BOM 3.11.2); parent [`selfcare-sdk-pom`](selfcare-sdk-pom.md) |
| Used by | `auth`, `delegation-cdc`, `document-ms`, `institution-cdc`, `onboarding-ms`, `product`, `user-cdc`, `user-group-cdc`, `user-ms`, `webhook` |

## What it provides

Package `it.pagopa.selfcare.commons.health`:

| Class | Purpose |
|---|---|
| `AbstractAsyncReadinessCheck` | `AsyncHealthCheck` with per-check timeout, error data and latency reporting; subclasses implement `checkName()` and `probe()`. |
| `AbstractBlobStorageReadinessCheck` | Adds `account`, `container`, optional `probeTarget` to the response data. |
| `AbstractMongoReadinessCheck` | Adds `database` and optional `host`; `hostFromConnectionString(...)` strips credentials from a Mongo URI. |
| `HealthCheckConstants` | Data keys and default timeout (2 s). |

A failing probe makes `/q/health/ready` return `DOWN` with a `data.error` field.
`probe()` runs on the Vert.x event loop: blocking clients must be wrapped with `runSubscriptionOn(...)`.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-health</artifactId>
  <version>0.2.0</version>
</dependency>
<!-- the app also needs io.quarkus:quarkus-smallrye-health -->
```

Annotate the concrete check with `@Readiness @ApplicationScoped`; override `timeout()` to change the default.
Full example in `libs/selfcare-sdk-health/README.md`.

## Build and release

```bash
mvn -f libs/selfcare-sdk-health/pom.xml clean verify
```

Release: `.github/workflows/release_health_sdk.yml` ("Release Health SDK"), push on `main` or `workflow_dispatch`;
builds with `mvn --projects :selfcare-sdk-health --also-make-dependents clean package -DskipTests` and deploys with
`mvn -B deploy -f libs/selfcare-sdk-health/pom.xml` to `https://maven.pkg.github.com/pagopa/selfcare`.
Coverage: profile `selfcare-sdk-health` in `test-coverage/pom.xml`.

## References

- [`libs/selfcare-sdk-health/README.md`](../../libs/selfcare-sdk-health/README.md)
- Spring Boot counterpart: [selfcare-sdk-health-spring](selfcare-sdk-health-spring.md)
