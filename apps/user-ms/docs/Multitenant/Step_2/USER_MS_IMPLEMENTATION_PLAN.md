# User MS Multitenancy - Step 2 Implementation Plan

## Objective

Apply the requirements defined in `apps/docs/Multitenant/Step_2` to `user-ms`,
using `onboarding-ms` as the reference implementation for tenant resolution,
MongoDB routing, JWT key selection, Azure Blob Storage routing, health checks,
and infrastructure configuration.

The implementation must preserve tenant identity across synchronous requests,
database operations, outbound REST calls, notifications, email, and
asynchronous event consumers.

## 1. Dependencies and tenant configuration

### Files to update

- `apps/user-ms/pom.xml`
- `apps/user-ms/src/main/resources/application.properties`
- `apps/user-ms/src/test/resources/application.properties`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/conf/MongoConfig.java`

### Implementation

1. Align the following dependencies and versions with `onboarding-ms`:
   - `selfcare-sdk-security`
   - `selfcare-sdk-tenant`
   - `selfcare-sdk-tenant-mongodb`
2. Replace the flat MongoDB, JWT, and product-storage configuration with:
   - `tenant.registry.json`
   - `tenant.supported-tenants`
   - tenant enforcement and default-tenant settings
   - tenant-specific MongoDB connection variables
   - tenant-specific JWT verification keys
   - tenant-specific storage bindings
   - mandatory `products` storage binding
3. Configure `selcUser` as the MongoDB database for each tenant.
4. Retain `user-ms.blob-storage.filepath-product=products.json` as the logical
   product catalogue path.
5. Disable the default MongoDB readiness check when readiness is handled by the
   tenant-aware implementation.
6. Convert `MongoConfig` into a CDI `CodecProvider` exposing the existing
   `DateCodec`. The tenant MongoDB producer must include this provider when it
   builds the Panache codec registry.
7. Configure at least two tenants in test resources, with independent MongoDB,
   JWT, and product-storage settings.

### Reference

- `apps/onboarding-ms/pom.xml`
- `apps/onboarding-ms/src/main/resources/application.properties`
- `apps/onboarding-ms/src/test/resources/application.properties`
- `libs/selfcare-sdk-tenant-mongodb/src/main/java/it/pagopa/selfcare/tenant/mongodb/TenantMongoClientProducer.java`

## 2. Request tenant resolution and propagation

### Components to add or adapt

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/TenantResolutionFilter.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/TenantLogUtils.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/CustomLoggingFilter.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/client/auth/AuthenticationPropagationHeadersFactory.java`

### Implementation

1. Port `TenantResolutionFilter` from `onboarding-ms`, adapting package names.
2. Resolve and normalize the incoming tenant through `TenantRegistry`.
3. Reject requests when the tenant is missing, unknown, duplicated, or
   inconsistent with another tenant identity.
4. Bypass tenant enforcement only for the required health endpoints.
5. Allow default-tenant fallback only when explicitly enabled for local or
   development environments.
6. Store the validated tenant in `TenantContext`.
7. Sanitize tenant values before writing them to logs.
8. Update `AuthenticationPropagationHeadersFactory` to propagate the normalized
   tenant from `TenantContext`, rather than forwarding the unvalidated request
   header.
9. Continue propagating authorization headers to the onboarding and webhook
   clients.
10. Resolve JWT verification keys through the tenant registry. Keep
    `mp.jwt.verify.publickey` only as a temporary compatibility fallback if
    required by the deployment sequence.
11. Require distinct JWT `kid` values before enabling both tenants in the same
    deployment.

### Tests

- Add `TenantResolutionFilterTest`.
- Extend `AuthenticationPropagationHeadersFactoryTest`.
- Cover:
  - valid and normalized tenants
  - missing tenants
  - unknown tenants
  - duplicated or conflicting tenant identities
  - health endpoint bypass
  - development-only defaulting
  - propagation from `TenantContext`
  - missing tenant context

### Reference

- `apps/onboarding-ms/src/main/java/it/pagopa/selfcare/onboarding/filter/TenantResolutionFilter.java`
- `apps/onboarding-ms/src/main/java/it/pagopa/selfcare/onboarding/filter/TenantLogUtils.java`
- `apps/docs/Multitenant/Step_2/JWT_Key_Resolution.md`
- `apps/docs/Multitenant/Step_2/SECURITY.md`

## 3. MongoDB tenant isolation

### Files and components

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/entity/UserInstitution.java`
- New `apps/user-ms/src/main/java/it/pagopa/selfcare/user/repository/UserInstitutionRepository.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserInstitutionServiceDefault.java`

### Implementation

1. Add a mandatory `tenantId` field to `UserInstitution`.
2. Create a tenant-aware `UserInstitutionRepository`, using
   `OnboardingRepository` as the reference.
3. Make the repository the mandatory access point for:
   - lookups by ID
   - queries
   - counts
   - inserts and upserts
   - single-record updates
   - bulk updates
4. Add the current tenant predicate to every database operation.
5. Stamp the current tenant on newly persisted entities.
6. Reject writes when an entity already contains a different tenant.
7. Replace direct static Panache and `mongoCollection()` usage in
   `UserInstitutionServiceDefault` with repository methods.
8. Ensure bulk updates cannot omit the tenant predicate.

### Compatibility rollout

During migration, support records that have not yet been backfilled:

```text
tenantId = currentTenant OR tenantId is missing/null
```

Enable this behavior only while `selfcare.tenant.strict-data-isolation=false`.
After both tenant datasets have been backfilled and verified, switch to exact
tenant equality and eventually remove the compatibility branch.

### Indexes

Add tenant-prefixed indexes to each `user-ms` infrastructure stack:

```text
(tenantId, institutionId)
(tenantId, userId, institutionId)
```

Update all environment and tenant combinations under:

```text
infra/resources/user-ms/
```

Keep MongoDB `_id` unique. Detect and resolve duplicate identifiers before
merging data from previously separate databases.

### Tests

- Add `UserInstitutionRepositoryTest`.
- Extend `UserInstitutionServiceTest`.
- Cover:
  - tenant predicates on every read and write path
  - tenant stamping
  - cross-tenant entity rejection
  - bulk update isolation
  - tenant-aware counts
  - compatibility mode
  - strict isolation mode
  - interleaved AR and PNPG operations

### Reference

- `apps/onboarding-ms/src/main/java/it/pagopa/selfcare/onboarding/repository/OnboardingRepository.java`
- `apps/docs/Multitenant/Step_2/Database_identification.md`
- `apps/docs/Multitenant/Step_1/scripts/README.md`

## 4. Tenant-aware product storage

### Components to port

- `StorageKeys`
- `PrefixingAzureBlobClient`
- `TenantBlobClientProvider`
- `TenantAwareProductService`

Create the corresponding implementations under:

```text
apps/user-ms/src/main/java/it/pagopa/selfcare/user/storage/
```

### Files to update

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/conf/UserMsConfig.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/health/ProductBlobStorageReadinessCheck.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/health/UserMongoReadinessCheck.java`

### Implementation

1. Resolve the Azure Blob Storage client from the current tenant's mandatory
   `products` binding.
2. Isolate cached clients by tenant and logical storage key.
3. Apply tenant-specific blob prefixes where configured.
4. Support the same storage authentication modes as `onboarding-ms`.
5. Remove the legacy product/client producers and flat storage fields from
   `UserMsConfig`.
6. Retain unrelated producers, including telemetry configuration.
7. Update product-storage readiness to check every configured tenant.
8. Update MongoDB readiness to ping every tenant-selected client and database.
9. Fail startup or readiness when a mandatory tenant binding is absent or
   invalid.

`user-ms` only consumes the product catalogue. It does not require
`onboarding-ms`'s dedicated product-database behavior.

### Tests

Port and adapt the onboarding storage tests to cover:

- different storage accounts per tenant
- multiple logical storage keys
- cache isolation
- missing credentials
- missing mandatory bindings
- supported authentication modes
- interleaved tenant product-catalogue reads
- tenant-aware readiness checks

### Reference

- `apps/onboarding-ms/src/main/java/it/pagopa/selfcare/onboarding/storage/`
- `apps/onboarding-ms/src/test/java/it/pagopa/selfcare/onboarding/storage/`
- `apps/docs/Multitenant/Step_2/Storage_identification.md`

## 5. Notifications, events, and email

### Event models

Add `tenantId` as an additive field to:

- `libs/selfcare-user-sdk-event/src/main/java/it/pagopa/selfcare/user/model/UserNotificationToSend.java`
- `libs/selfcare-user-sdk-event/src/main/java/it/pagopa/selfcare/user/model/FdUserNotificationToSend.java`

Update:

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/mapper/NotificationMapper.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserNotificationServiceImpl.java`

### Notification implementation

1. Map `tenantId` from `UserInstitution` into every notification.
2. Preserve that immutable tenant identity through all asynchronous handoffs.
3. Remove fixed `user-ms.webhook.tenant-id` behavior.
4. Build webhook requests using the tenant carried by the notification.
5. Reject tenant-less notifications before invoking downstream services.

### Internal event consumers

Update:

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/internalevents/InternalEvents.java`

For each callback:

1. Read the tenant from the event.
2. Validate it through `TenantRegistry`.
3. Activate an isolated request context where required.
4. Set `TenantContext` before any database or storage operation.
5. Clear the context when processing completes or fails.
6. Reject missing or unknown tenants before accessing data.

Do not reuse tenant context across concurrent callbacks.

### Upstream dependency

`institution-cdc` currently has an AR-specific tenant mapping. It must publish
the actual tenant before PNPG traffic can be enabled in a shared deployment.

Relevant component:

```text
apps/institution-cdc/src/main/java/it/pagopa/selfcare/institution/event/internalevents/InternalEventsMapper.java
```

### Email

Update:

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/OneMailServiceImpl.java`

Requirements:

1. Select sender, credentials, templates, and dashboard URLs per tenant.
2. Keep email disabled for a tenant until all required mappings exist.
3. Fail closed instead of reusing another tenant's configuration.
4. Do not log recipients or other personal information.
5. Surface terminal failures according to the configured retry policy instead
   of silently suppressing them.

No Personal Data Vault change is required because `user-ms` does not call the
vault.

### Tests

Cover:

- tenant retention in mapped notifications
- tenant selection for webhook calls
- rejection of notifications without a tenant
- invalid event tenants
- context cleanup after success and failure
- concurrent callbacks for different tenants
- disabled or incomplete email mappings
- prevention of cross-tenant credential reuse

## 6. Infrastructure configuration

### Canonical tenant metadata

Add canonical `tenant_data_isolation` metadata to:

```text
infra/resources/_modules/local-env/locals.tf
```

Use this metadata to generate tenant registry JSON. Avoid duplicating literal
registry definitions across every application stack.

### User MS deployment variables

Provide the following in every `user-ms` stack:

```text
TENANT_SUPPORTED_TENANTS
TENANT_REGISTRY_JSON
JWT_PUBLIC_KEY_<TENANT>
MONGODB_CONNECTION_STRING_<TENANT>
```

Also provide tenant-specific storage credentials or managed identities.

Remove legacy flat MongoDB, JWT, webhook-tenant, and storage settings only in
the same rollout that deploys the compatible application revision.

### Secret ownership

Choose one of these approaches before consolidation:

1. Use a shared Key Vault containing the required tenant secrets.
2. Extend the Container App module to resolve secrets from more than one vault.

The existing module assumes a single vault, so this decision is a deployment
prerequisite.

## 7. Data migration

Run the existing Step 1 backfill separately for AR and PNPG against:

- `selcUser.userInstitutions`
- the separately owned `userInfo` collection

### Migration sequence

1. Deploy code supporting compatibility reads.
2. Back up both source datasets.
3. Detect duplicate `_id` values before consolidation.
4. Backfill `tenantId` in each tenant's records.
5. Run the migration verification mode.
6. Confirm there are no records with missing, null, unknown, or incorrect
   tenant identifiers.
7. Enable strict data isolation.
8. Monitor tenant-specific query and update failures.
9. Remove compatibility reads only after every environment is strict.

## 8. Recommended delivery order

### Phase 1: Shared configuration

- Align dependencies.
- Add tenant registry configuration.
- Integrate the user-specific MongoDB codec.
- Add tenant-aware test configuration.

### Phase 2: Request security

- Add request tenant resolution.
- Add sanitized tenant logging.
- Add JWT selection.
- Add validated outbound tenant propagation.

### Phase 3: Database isolation

- Add `UserInstitution.tenantId`.
- Introduce `UserInstitutionRepository`.
- Remove direct MongoDB access from services.
- Add tenant-prefixed indexes.
- Enable compatibility mode.

### Phase 4: Storage and health

- Introduce tenant-aware product storage.
- Replace flat storage producers.
- Add tenant-aware MongoDB and blob readiness checks.

### Phase 5: Asynchronous flows

- Extend event schemas.
- Propagate tenant identity through notifications.
- Isolate tenant context in event callbacks.
- Make webhook calls tenant-aware.
- Gate email by tenant configuration.

### Phase 6: Migration and strict mode

- Backfill AR and PNPG datasets.
- Verify migrated data.
- Enable strict isolation.

### Phase 7: Infrastructure consolidation

- Enable both tenants in the shared deployment.
- Verify downstream consumers.
- Consolidate network, secrets, and Terraform ownership.
- Retain legacy stacks temporarily for rollback.

## 9. Validation gates

The shared deployment must not be enabled until all of the following pass:

- Startup fails when mandatory tenant configuration is missing.
- MongoDB readiness succeeds for every configured tenant.
- Product-storage readiness succeeds for every configured tenant.
- JWTs issued for both tenants are validated with their respective keys.
- Every repository operation contains a tenant predicate.
- Cross-tenant reads, writes, counts, and bulk updates are rejected.
- Outbound REST calls carry the normalized tenant.
- Events and notifications preserve tenant identity.
- Concurrent consumers do not leak tenant context.
- Email cannot reuse another tenant's configuration.
- Secret rotation succeeds through a new application revision.
- Backfill verification reports no invalid records.

## 10. Consolidation blockers

Resolve these before routing both tenants through one `user-ms` deployment:

1. `institution-cdc` must emit the actual tenant rather than a fixed AR value.
2. Downstream onboarding and webhook services must accept tenant-bearing
   traffic.
3. Tenant-specific email configuration must be available, or email must remain
   disabled.
4. The shared application must have network access to both tenants' resources.
5. Secret ownership and Key Vault access must support both tenants.
6. Terraform state must have a single, explicit owner for the consolidated
   resources.
7. Both legacy deployments must remain available during the initial rollout so
   traffic routing can be rolled back.

## Definition of done

Step 2 is complete for `user-ms` when one deployment can safely serve all
configured tenants while:

- resolving tenant identity before business processing
- selecting the correct JWT key, MongoDB client, database, and product storage
- enforcing tenant predicates on every data operation
- preserving tenant identity through outbound and asynchronous flows
- exposing tenant-aware readiness checks
- operating in strict data-isolation mode after verified migration
- preventing configuration, credential, context, and data leakage between
  tenants
