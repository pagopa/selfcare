# selfcare-commons

> Multi-module Spring Boot library (`selc-commons`) shared by the SelfCare Spring services: base security/model types, JWT web security and error handling, Feign/SOAP connectors, digital signature utilities and tenant (multitenancy) support.

| | |
|---|---|
| Path | `libs/selfcare-commons` |
| Artifact | `it.pagopa.selfcare:selc-commons:2.23.0` (packaging `pom`); modules listed below |
| Stack | Spring Boot (parent [`selc-starter-parent:0.3.3`](selfcare-starter-parent.md), Spring Boot 3.3.0), Spring Cloud OpenFeign, Java 17, blocking |
| Used by | `dashboard-bff` (2.10.0), `external-api` (2.23.0), `institution-ms` (2.22.0), `onboarding-bff` (2.10.0), `registry-proxy` (2.23.0), `user-group-ms` (2.20.0) — versions from the `selc-commons.version` property of each app POM |

## Modules

| Module (artifactId) | Path | What it provides | Used by (apps) |
|---|---|---|---|
| `selc-commons-base` | `base/` | `SelfCareUser`, `ServiceAccount`, `SelfCareAuthority`, `ProductGrantedAuthority`, `PartyRole`; enums `Env`, `InstitutionType`, `Origin`, `ProductId`, ...; `TargetEnvironment` (from `ENV_TARGET`); `LogUtils.CONFIDENTIAL_MARKER`; `PerformanceLoggingConfig` aspect (`performance.logger.enabled`, env `PERFORMANCE_LOGGER_ENABLED`); `logback-spring.xml` with JSON logging | dashboard-bff, external-api, institution-ms, onboarding-bff, registry-proxy, user-group-ms |
| `selc-commons-web` | `web/` | Spring Security JWT stack: `JwtAuthenticationFilter`, `JwtAuthenticationProvider`, strategies `SpidJwtAuthenticationStrategy`, `PagopaJwtAuthenticationStrategy`, `K8sJwtAuthenticationStrategy`, `JwtService`, `SecurityConfig`; `RestExceptionsHandler`/`FeignExceptionsHandler` returning `Problem`; `Page`/`PageMapper`; springdoc configs (`BaseSwaggerConfig`, `ServerSwaggerConfig`, ...); `LogRequestInterceptor`; `ControllerResponseValidator`; auto-configuration `AuthoritiesRetrieverAutoconfiguration` | dashboard-bff, external-api, institution-ms, onboarding-bff, registry-proxy, user-group-ms |
| `selc-commons-tenant` | `tenant/` | Spring version of the tenant registry: `TenantConfiguration` (beans `TenantContext` — `ThreadLocal` based — and `TenantRegistry`), `TenantDefinition`; `mongodb/TenantMongoDatabaseFactory` (enabled by `tenant.mongodb.enabled=true`); `storage/TenantBlobClientProvider`, `TenantBlobClient` (enabled by `tenant.storage.enabled=true`) | external-api, institution-ms, registry-proxy, user-group-ms |
| `selc-commons-connector` | `connector/` | Aggregator POM for `rest` and `soap` | — |
| `selc-commons-connector-rest` | `connector/rest/` | Feign base config `RestClientBaseConfig` (`default-rest-client.properties`: `REST_CLIENT_CONNECT_TIMEOUT`, `REST_CLIENT_READ_TIMEOUT`, `REST_CLIENT_LOGGER_LEVEL`); interceptors `AuthorizationHeaderInterceptor` (propagates `Authorization` and `X-Tenant-Id`), `PartyTraceIdInterceptor` (`X-Correlation-Id` from MDC `traceId`), `QueryParamsPlusEncoderInterceptor`; `PageJacksonModule` | dashboard-bff, external-api, institution-ms, onboarding-bff, registry-proxy |
| `selc-commons-connector-soap` | `connector/soap/` | Aggregator with JAX-WS/JAXB/SAAJ dependencies | — |
| `selc-commons-connector-soap-aruba-sign` (groupId `it.pagopa.selfcare.soap`) | `connector/soap/aruba-sign/` | Aruba ARSS SOAP client (`ArubaSignService`, `ArubaPkcs7HashSignServiceImpl`), WSDL in `src/main/resources/docs/aruba/`, properties `aruba.sign-service.*`; active when `crypto.pkcs7.source=aruba` | institution-ms |
| `selc-commons-connector-soap-utils` (groupId `it.pagopa.selfcare.soap`) | `connector/soap/utils/` | `SoapLoggingHandler` | (transitive via aruba-sign) |
| `selc-common-utils` | `utils/` | Aggregator for utility modules | — |
| `selc-commons-crypto` (groupId `it.pagopa.selfcare.utils`) | `utils/crypto/` | `PadesSignService`, `CadesSignService`, `Pkcs7HashSignService` (PDFBox, BouncyCastle); config `crypto.pkcs7.source` (`CRYPTO_PKCS7_SOURCE`, default `local`), `CRYPTO_PRIVATE_KEY`, `CRYPTO_CERT` | institution-ms |

"Used by" lists apps whose `pom.xml` (any module) declares the artifactId, including `dependencyManagement` entries.

### Main configuration (web module)

`web/src/main/resources/config/jwt.properties` sets `jwt.signingKey` from `JWT_TOKEN_PUBLIC_KEY`. Other properties read by
the security classes: `jwt.issuer`, `jwt.audience`, `jwt.tenant-claim-required` (default `false`),
`jwt.tenant-audience-resolution-enabled` (default `false`), `tenant.default` (default `PNPG`), `tenant.pagopa.default` (default `AR`).
The tenant module reads `tenant.registry.json`, `tenant.supported-tenants`, `tenant.storage.mandatory-keys`,
`tenant.one-identity.mandatory-tenants`, `tenant.user-registry.mandatory-tenants`, `tenant.mongodb.application-name`.

### Non-Java content

- `github-actions-template/` — composite actions `create-release`, `promote-release`, `swagger-detect-conflict-quarkus`,
  `swagger-detect-conflict-spring` (the root workflow `.github/workflows/release_open_api.yml` still references
  `pagopa/selfcare-commons/github-actions-template/swagger-detect-conflict-spring@main`).
- `infra/terraform-modules/` — `container_app_microservice`, `github_repository_settings` (each with README).
- `.github/` and `.devops/` — CI files imported from the original `selfcare-commons` repository; GitHub Actions only runs
  workflows under the monorepo root `.github/workflows/`.

## Usage

```xml
<properties>
  <selc-commons.version>2.23.0</selc-commons.version>
</properties>
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selc-commons-web</artifactId>
  <version>${selc-commons.version}</version>
</dependency>
```

## Build and release

```bash
mvn -f libs/selfcare-commons/pom.xml clean verify
mvn --projects :selc-commons --also-make-dependents clean package -DskipTests
```

Release: `.github/workflows/release_selfcare_commons.yml` ("Release Selfcare Commons"), push on `main` or
`workflow_dispatch`; deploys with `mvn -B deploy -f libs/selfcare-commons/pom.xml` to
`https://maven.pkg.github.com/pagopa/selfcare` (`distributionManagement` in `libs/selfcare-commons/pom.xml`).
All modules share the version of the root POM (`2.23.0`).

## References

- `libs/selfcare-commons/utils/crypto/README.md`, `libs/selfcare-commons/connector/soap/aruba-sign/README.md`
- Quarkus counterparts: [selfcare-sdk-security](selfcare-sdk-security.md), [selfcare-sdk-tenant](selfcare-sdk-tenant.md), [selfcare-onboarding-sdk-crypto](selfcare-onboarding-sdk-crypto.md)
- [Multitenancy](../multitenancy.md)
