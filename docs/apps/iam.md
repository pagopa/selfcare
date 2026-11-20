# iam

> Identity and Access Management microservice: stores internal (SSO) users and the product roles/permissions of SSO and SPID users, and answers permission checks.

| | |
|---|---|
| Path | `apps/iam` |
| Stack | Quarkus 3.31.2, Java 17, reactive (Mutiny `Uni`, reactive Mongo Panache) |
| Type | REST microservice |
| Tenants | Both: one deployment per tenant (`iam-ms` for AR, `pnpg-iam-ms` for PNPG) |
| Infra | `infra/resources/iam/` (`dev-ar`, `uat-ar`, `prod-ar`, `dev-pnpg`, `uat-pnpg`, `prod-pnpg`) |

## Responsibilities

- Create/update internal users and their product roles (`PATCH /iam/users`).
- Look up users by id, by e-mail, or by product (`GET /iam/users*`).
- Resolve role → permission mappings and answer "has permission" checks.
- Encrypt user e-mails with the key of the tenant that created the user (`util/DataEncryptionConfig`, `selfcare-onboarding-sdk-crypto`).
- Resolve the products an institution is onboarded to through institution-ms (`IamServiceImpl.getInstitutionProducts`), used to scope permission checks by `institutionId`.

The data model (`userClaims`, `roles`, embedded `productRoles`) and the permission aggregation pipeline are described in [`apps/iam/Readme.md`](../../apps/iam/Readme.md).

## Architecture

Package root: `apps/iam/src/main/java/it/pagopa/selfcare/iam/`.

| Package | Key classes |
|---|---|
| `controller` | `IamController` (`@Authenticated`, base path `/iam`) |
| `service` | `IamService`, `IamServiceImpl` |
| `repository` | `UserClaimsRepository`, `UserPermissionsRepository`, `TenantDataFilter` |
| `entity` | `UserClaims` (`userClaims`), `Roles` (`roles`) |
| `model` | `ProductRoles`, `ProductRole`, `UserPermissions`, `ProductRolePermissions*`, `Role` |
| `security` | `IamSecurityIdentityAugmentor` (extends `JWTSecurityIdentityAugmentor` from `selfcare-sdk-security`) |
| `filter` | `TokenPropagationFilter` (outbound JWT), logging filters |
| `config`, `conf` | `OpenApiSecurityConfig`, `IamConfig`, `JacksonConfiguration` |
| `exception/handler` | `ExceptionHandler` |

## API

Base path `/iam`, all endpoints require a JWT (`@Authenticated`). Spec: `apps/iam/src/main/docs/openapi.json` / `openapi.yaml`.

| Method | Path | Summary | Tag `external-v2` |
|---|---|---|---|
| GET | `/iam/ping` | Ping endpoint | no |
| PATCH | `/iam/users` | Saves IAM User | yes |
| GET | `/iam/users/{uid}` | Get IAM User | yes |
| GET | `/iam/users/search` | Get IAM User by Email | yes |
| GET | `/iam/users` | Get IAM Users (by `productId`) | no |
| GET | `/iam/users/role/permissions/{uid}` | Get product role permissions list | yes |
| GET | `/iam/users/{uid}/roles/` | Get product roles | yes |
| GET | `/iam/users/{uid}/permissions/{permission}` | Check IAM User hasPermission | no |

Several endpoints accept an optional `tenantId` query parameter resolved by `EffectiveTenant` (`selfcare-sdk-security`): when omitted the tenant comes from `X-Tenant-Id` or the JWT `tenant_id` claim; a conflicting value is rejected with 400.

APIM exposure: operations tagged `external-v2` are merged into the external API `ms_external_api/v2` (`infra/resources/_modules/apim_external_api/api/ms_external_api/v2/openapi_merge/openapi_merge.json`, input `selfcare-iam-ms`), exposed under `/iam/users...`. See [architecture.md](../architecture.md).

## Data and messaging

- MongoDB (Cosmos DB) database `selcIam`, collections `userClaims` and `roles` (`infra/resources/iam/*/iam.tf`).
- The default Mongo client is disabled (`quarkus.mongodb.active=false`); `selfcare-sdk-tenant-mongodb` (`TenantMongoClientProducer`) selects the client/database from `TENANT_REGISTRY_JSON`.
- No Event Hub, queue or blob usage.

## Integrations

| Direction | Peer | Details |
|---|---|---|
| Outbound | institution-ms | Generated client from `apps/iam/src/main/openapi/institution_openapi.json` (`InstitutionApi`), URL `INSTITUTION_API_URL`, caller JWT propagated by `TokenPropagationFilter` |
| Inbound | [auth](auth.md) | Spec copy `apps/auth/src/main/openapi/iam.json` (`IAM_API_URL`) |
| Inbound | dashboard-bff | Spec copy `apps/dashboard-bff/docs/openapi/api-iam-docs.json` |
| Inbound | External products | Through APIM `ms_external_api/v2` |

## Configuration

From `apps/iam/src/main/resources/application.properties` and `infra/resources/iam/*/iam.tf` (names only):

| Group | Variables |
|---|---|
| Tenant | `TENANT_REGISTRY_JSON`, `TENANT_SUPPORTED_TENANTS`, `TENANT_DEFAULT`, `DEFAULT_TENANT` (default tenant for SPID tokens without `tenant_id`), `TENANT_ENFORCEMENT_ENABLED`, `SELFCARE_TENANT_STRICT_DATA_ISOLATION` |
| Mongo / JWT | `MONGODB_CONNECTION_STRING_<TENANT>`, `JWT_PUBLIC_KEY_<TENANT>` (referenced by the registry), `JWT_PUBLIC_KEY` (legacy fallback) |
| Encryption | `SELFCARE_DATA_ENCRIPTION_KEY_<TENANT>`, `SELFCARE_DATA_ENCRIPTION_IV_<TENANT>` |
| Outbound | `INSTITUTION_API_URL`, `IAM_MS_RETRY`, `IAM_MS_RETRY_MIN_BACKOFF`, `IAM_MS_RETRY_MAX_BACKOFF` |
| Observability | `APPLICATIONINSIGHTS_CONNECTION_STRING` |

Secrets come from Key Vault (`secrets_names_iam_ms` in `iam.tf`).

## Multitenancy

- `selfcare.tenant.resolution-filter.enabled=true`: the tenant is resolved by `it.pagopa.selfcare.security.tenant.TenantResolutionFilter` (`selfcare-sdk-security`) and stored in `TenantContext`. Each stack deploys a single tenant (`tenant_id_iam_ms = upper(domain)`), with `TENANT_ENFORCEMENT_ENABLED=false` because not every internal caller sends `X-Tenant-Id` yet (requests fall back to `TENANT_DEFAULT`).
- User identity is shared across tenants; isolation is per `productRoles[].tenantId`, applied by `repository/TenantDataFilter`. While `selfcare.tenant.strict-data-isolation=false`, legacy roles without `tenantId` remain visible (backfill pending).
- JWT verification uses per-tenant public keys from the registry (`jwt.publicKeyEnvVar`). See [multitenancy.md](../multitenancy.md) and [Step 2 Database identification](../specs/MULTITENANT/Step_2/Database_identification.md).

## Build, run and test

```bash
mvn --projects :iam --also-make package -DskipTests      # build + regenerate src/main/docs/openapi.*
mvn -f apps/iam/pom.xml test                             # unit tests (CucumberSuiteTest excluded)
mvn -f apps/iam/pom.xml quarkus:dev                      # local stack: apps/iam/docker-compose.yml
cd apps/iam && mvn test -Dtest=CucumberSuiteTest -Dsurefire.failIfNoSpecifiedTests=false   # Cucumber (Docker)
```

- Cucumber runner: `it.pagopa.selfcare.iam.cucumber.CucumberSuiteTest` (profile `CucumberTestProfile`); features `health-check.feature`, `permissions.feature`, `user-management.feature`.
- Note: `integration_tests_iam_ms.yml` runs `-Dtest=CucumberSuite`; check that Surefire reports `Tests run: N` with N > 0.

## Deployment

- Terraform per environment/tenant in `infra/resources/iam/<env>-<tenant>/iam.tf`: Cosmos database/collections and Container App (`selc-<env>-iam-ms` for AR, `selc-<env>-pnpg-iam-ms` for PNPG; image `selfcare-iam-ms`).
- Workflows: `pr_iam.yml`, `pr_iam_infra.yml` (plans the `*-ar` stacks), `integration_tests_iam_ms.yml`, `trigger_release_iam.yml` (target `ar`), generic `release_app.yml` (target `ar` or `pnpg`), `opex_api_iam.yml` (Opex dashboard). `trigger_deploy_after_merge.yml` triggers the DEV release on push to `main`; `create_release_branch.yml` creates `releases/<domain>_vX.Y.Z` and triggers the UAT release. See [ci-cd.md](../ci-cd.md).

## References

- [`apps/iam/Readme.md`](../../apps/iam/Readme.md)
- [MULTITENANT Step 2 – Architecture](../specs/MULTITENANT/Step_2/ARCHITECTURE.md), [JWT key resolution](../specs/MULTITENANT/Step_2/JWT_Key_Resolution.md)
- [selfcare-sdk-security](../libs/selfcare-sdk-security.md), [selfcare-sdk-tenant](../libs/selfcare-sdk-tenant.md), [selfcare-sdk-tenant-mongodb](../libs/selfcare-sdk-tenant-mongodb.md), [selfcare-onboarding-sdk-crypto](../libs/selfcare-onboarding-sdk-crypto.md)
- [testing.md](../testing.md), [infrastructure.md](../infrastructure.md)
