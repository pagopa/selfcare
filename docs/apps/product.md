# product

> Product catalogue microservice (product-ms): source of truth for product configurations, contract templates and onboarding admission rules.

| | |
|---|---|
| Path | `apps/product` |
| Stack | Quarkus 3.31.2, Java 17, reactive (Mutiny `Uni`, reactive Mongo Panache) |
| Type | REST microservice |
| Tenants | Both: one deployment per tenant (`product-ms` for AR, `pnpg-product-ms` for PNPG) |
| Infra | `infra/resources/product/` (`dev-ar`, `uat-ar`, `prod-ar`, `dev-pnpg`, `uat-pnpg`, `prod-pnpg`) |

## Responsibilities

- Create, read, patch and cancel product configurations (versioned per `productId`).
- Upload, list and download contract template versions (HTML/PDF) stored in blob storage.
- Evaluate onboarding admission rules: whether an `(institutionType, origin)` pair can join a product, through which channel and with which workflow (`service/OnboardingRules`), and validate product configurations on save.
- Expose required documents, product origins, expiration days and role-mapping validation to the onboarding flows and the frontends.

## Architecture

Package root: `apps/product/src/main/java/it/pagopa/selfcare/product/`.

| Package | Key classes |
|---|---|
| `controller` | `ProductController` (`/product`), `ContractTemplateController` (`/contract-template`), both `@Authenticated` |
| `service` | `ProductService`/`ProductServiceImpl`, `ContractTemplateService`/`ContractTemplateServiceImpl`, `OnboardingRules` |
| `repository` | `ProductRepository`, `ContractTemplateRepository` (reactive Panache) |
| `model` | `Product` (`products`), `ContractTemplate` (`contractTemplates`), workflow/signing/role-mapping models, enums |
| `storage` | `ContractTemplateStorage(Impl)`, `TenantBlobClientProvider`, `TenantBlobBinding`, `StorageKeys` |
| `config` | `ProductConfig`, `OnboardingRulesConfig`, `MongoConfig`, `OpenApiSecurityConfig` |
| `mapper` | MapStruct mappers (`ProductMapperRequest`, `ProductMapperResponse`, `ContractTemplateMapper`) |
| `validator` | `AllowedFileTypes`, `UserValidator` |
| `health` | `ProductMongoReadinessCheck` |

## API

All endpoints require a JWT. Spec: `apps/product/src/main/docs/openapi.json` / `openapi.yaml`.

| Method | Path | Summary | Extra tags |
|---|---|---|---|
| GET | `/product/ping` | Ping | |
| POST | `/product` | Create or update a product configuration | `external-v2`, `external-pnpg` |
| GET | `/product` | Get products (latest version per `productId`) | |
| GET / PATCH / DELETE | `/product/{productId}` | Get / partially update / cancel product | `external-v2`, `external-pnpg` |
| GET | `/product/origins` | Get product origins | `external-v2`, `external-pnpg` |
| GET | `/product/workflow-type` | Resolve workflow type | |
| POST | `/product/{productId}/onboarding-decision` | Decide whether an institution can onboard | |
| HEAD | `/product/{productId}/required-documents/enabled` | Required documents enabled (header `X-Required-Documents-Enabled`) | |
| GET | `/product/{productId}/required-documents` | Required documents | |
| GET | `/product/{productId}/valid` | Product only if valid for onboarding | |
| GET | `/product/{productId}/expiration-days` | Expiration days | |
| GET | `/product/{productId}/role-mappings/validate` | Validate a product role | |
| POST | `/contract-template` | Upload a contract template version | `external-v2` |
| GET | `/contract-template/{contractTemplateId}` | Download a contract template version | `external-v2` |
| GET | `/contract-template` | List contract templates | `external-v2` |

APIM exposure (external APIs, `infra/resources/_modules/apim_external_api/`):

- `external-v2` operations are merged into `api/ms_external_api/v2` (input `selfcare-product-ms`).
- `external-pnpg` operations are merged into `api_pnpg/external_api_for_pnpg/v2`.

## Data and messaging

- MongoDB (Cosmos DB) database `selcProduct`, collections `products` and `contractTemplates` (`infra/resources/product/*/product.tf`). The Mongo client is selected per tenant by `selfcare-sdk-tenant-mongodb` (`quarkus.mongodb.active=false`).
- Blob storage: contract templates go to the tenant storage binding `contracts` (AR: documents storage account, container `sc-<env>-documents-blob`). PNPG has no contract storage (`TENANT_STORAGE_MANDATORY_KEYS` empty), see [`apps/product/Readme.md`](../../apps/product/Readme.md).
- Product changes are propagated by [product-cdc](product-cdc.md) from the `products` change stream.

## Integrations

| Direction | Peer | Details |
|---|---|---|
| Inbound | onboarding-ms | Spec copy `apps/onboarding-ms/src/main/openapi/product.json`, env `MS_PRODUCT_URL` |
| Inbound | onboarding-functions | Spec copy `apps/onboarding-functions/src/main/openapi/product.json`, env `MS_PRODUCT_URL` |
| Inbound | External products | Through APIM `ms_external_api/v2` and PNPG `external_api_for_pnpg/v2` |

No outbound REST clients (no `src/main/openapi/` folder).

## Configuration

From `apps/product/src/main/resources/application.properties` and `product.tf` (names only):

| Group | Variables / properties |
|---|---|
| Tenant | `TENANT_REGISTRY_JSON`, `TENANT_SUPPORTED_TENANTS`, `TENANT_ENFORCEMENT_ENABLED`, `TENANT_DEFAULT`, `TENANT_STORAGE_MANDATORY_KEYS` |
| Mongo / JWT | `MONGODB_CONNECTION_STRING_<TENANT>`, `JWT_PUBLIC_KEY_<TENANT>`, `JWT_PUBLIC_KEY` (legacy fallback), `MONGODB_DATABASE_NAME` |
| Storage | `BLOB_STORAGE_CONNECTION_STRING_CONTRACT_TEMPLATE_AR`, `BLOB_STORAGE_CONTAINER_CONTRACT_TEMPLATE`, `CONTRACT_TEMPLATE_LOGO_BASE_URL` |
| Onboarding rules | `ONBOARDING_RULES_VALIDATION_MODE` (`STRICT` rejects incoherent products with 400, `WARN` logs), `product-ms.onboarding-rules.allowed-institution-types.*`, `product-ms.onboarding-rules.allowed-workflow-types.*` |
| Encryption | `SELFCARE_DATA_ENCRIPTION_KEY`, `SELFCARE_DATA_ENCRIPTION_IV` |
| Misc | `PRODUCT_MS_RETRY`, `PRODUCT_MS_RETRY_MIN_BACKOFF`, `PRODUCT_MS_RETRY_MAX_BACKOFF`, `APPLICATIONINSIGHTS_CONNECTION_STRING` |

## Multitenancy

- `selfcare.tenant.resolution-filter.enabled=true` (`selfcare-sdk-security`) resolves the tenant into `TenantContext`; per-tenant JWT public keys, Mongo routes and storage bindings come from `TENANT_REGISTRY_JSON`.
- Each stack configures only its own tenant in the registry; PNPG clears the mandatory `contracts` storage key.
- See [multitenancy.md](../multitenancy.md) and [Step 2 Storage identification](../specs/MULTITENANT/Step_2/Storage_identification.md).

## Build, run and test

```bash
mvn --projects :product --also-make package -DskipTests   # build + regenerate src/main/docs/openapi.*
mvn -f apps/product/pom.xml test                          # unit tests (CucumberSuiteTest excluded)
mvn -f apps/product/pom.xml quarkus:dev
mvn --projects :product --also-make test -Dtest=CucumberSuiteTest -Dsurefire.failIfNoSpecifiedTests=false   # Cucumber (Docker)
```

- Cucumber runner: `it.pagopa.selfcare.product.integrationTest.CucumberSuiteTest`; features `product.feature`, `contract_template.feature`, `onboarding_decision.feature`.
- After changing the API, refresh the spec copies in onboarding-ms and onboarding-functions.

## Deployment

- Terraform in `infra/resources/product/<env>-<tenant>/product.tf`: Cosmos database/collections and Container App (`selc-<env>-product-ms` / `selc-<env>-pnpg-product-ms`, image `selfcare-product-ms`).
- Workflows: `pr_product.yml`, `pr_product_infra.yml` (plans `*-ar`), `integration_tests_product_ms.yml`, `trigger_release_product.yml` (releases both `ar` and `pnpg`), `release_app.yml`, `opex_api_product.yml`. `trigger_deploy_after_merge.yml` triggers the DEV release on push to `main`; `create_release_branch.yml` creates `releases/<domain>_vX.Y.Z` and triggers the UAT release. See [ci-cd.md](../ci-cd.md).

## References

- [`apps/product/Readme.md`](../../apps/product/Readme.md)
- [ONBOARDING_ADMISSION_RULES – Requirements](../specs/ONBOARDING_ADMISSION_RULES/REQUIREMENTS.md), [Architecture](../specs/ONBOARDING_ADMISSION_RULES/ARCHITECTURE.md)
- [product-cdc](product-cdc.md), [selfcare-sdk-tenant](../libs/selfcare-sdk-tenant.md), [selfcare-sdk-tenant-mongodb](../libs/selfcare-sdk-tenant-mongodb.md), [selfcare-sdk-security](../libs/selfcare-sdk-security.md)
- [testing.md](../testing.md), [infrastructure.md](../infrastructure.md)
