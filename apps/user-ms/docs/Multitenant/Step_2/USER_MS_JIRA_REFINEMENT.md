# User MS Step 2 Multitenancy - Jira Refinement Backlog

## Epic: make `user-ms` multitenant

### Objective

Move `user-ms` to a single application instance capable of serving the AR and
PNPG tenants while keeping authentication, MongoDB data, storage,
notifications, email configurations, and asynchronous contexts separate.

### Expected outcome

A request or event can access only the resources associated with its own
tenant. The application must fail explicitly when the tenant is missing,
unknown, or incompletely configured.

---

## USRMT-01 - Configure the SDK and tenant registry in `user-ms`

**Type:** Technical story

### Objective

Configure `user-ms` to use the shared tenant registry to resolve MongoDB, JWT
keys, and Azure Blob Storage for AR and PNPG.

### Context

`user-ms` currently uses flat properties for MongoDB, JWT, and product storage.
The application reference is the multitenant configuration already present in
`onboarding-ms`.

### Components involved

- `apps/user-ms/pom.xml`
- `apps/user-ms/src/main/resources/application.properties`
- `apps/user-ms/src/test/resources/application.properties`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/conf/MongoConfig.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/conf/DateCodec.java`

### Tasks

1. Align the following dependencies with the versions used by `onboarding-ms`:
   - `selfcare-sdk-security`
   - `selfcare-sdk-tenant`
   - `selfcare-sdk-tenant-mongodb`
2. Configure:
   - `tenant.registry.json`
   - `tenant.supported-tenants`
   - tenant enforcement
   - an optional default tenant, limited to local environments
   - MongoDB, JWT, and storage bindings for AR and PNPG
   - required `products` storage binding
3. Set `selcUser` as the MongoDB database for each tenant.
4. Retain `user-ms.blob-storage.filepath-product=products.json` as the logical
   name of the product catalog.
5. Disable the standard MongoDB health check when it is replaced by the
   multitenant check.
6. Make `MongoConfig` a CDI `CodecProvider` that exposes `DateCodec`, so that
   the multitenant MongoDB producer includes it in the codec registry.
7. Create a test configuration with two tenants and independent resources.

### Acceptance criteria

- `user-ms` starts with a valid registry containing AR and PNPG.
- Each tenant resolves its own `selcUser` database, JWT key, and `products`
  binding.
- Startup fails if a required configuration is missing.
- `DateCodec` continues to be used by the MongoDB clients generated per tenant.
- Tests verify two independent tenant configurations.

### Required tests

- Startup test with a valid registry.
- Failed startup test with a missing MongoDB, JWT, or `products` binding.
- `DateCodec` registration and usage test.

**Dependencies:** none.

---

## USRMT-02 - Resolve and validate the tenant for HTTP requests

**Type:** Security story

### Objective

Validate the tenant before an HTTP request can reach controllers, services,
repositories, or storage.

### Context

The tenant received from the client cannot be considered trusted. It must be
normalized and checked against `TenantRegistry`, replicating the multitenant
behavior of `onboarding-ms`.

### Components involved

- New `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/TenantResolutionFilter.java`
- `TenantRegistry`
- `TenantContext`

### Tasks

1. Implement an HTTP filter with sufficient priority to perform validation
   before application logic.
2. Extract and normalize the tenant identifier.
3. Verify that the tenant is included in `tenant.supported-tenants` and present
   in the registry.
4. Reject missing, unknown, duplicate, or conflicting tenants.
5. Store only the normalized value in `TenantContext`.
6. Exclude only the designated health endpoints from enforcement.
7. Allow a default tenant only when the corresponding option is explicitly
   enabled in a local or development environment.
8. Ensure the context is cleared at the end of the request.

### Acceptance criteria

- A request with a valid tenant reaches the controller with `TenantContext`
  populated.
- A request without a tenant is rejected when enforcement is enabled.
- Unknown or conflicting tenants are rejected before any MongoDB or storage
  access.
- Health endpoints remain accessible without a tenant.
- Default-tenant fallback is not available in production.
- The context is not reused by subsequent requests.

### Required tests

- Valid, normalized tenant.
- Missing tenant.
- Unknown tenant.
- Duplicate or conflicting tenant.
- Health endpoint bypass.
- Local fallback enabled and disabled.
- `TenantContext` cleanup.

**Dependencies:** USRMT-01.

---

## USRMT-03 - Resolve the JWT key based on the tenant

**Type:** Security story

### Objective

Validate each JWT using only the key configured for the request tenant.

### Context

In a shared deployment, AR and PNPG may use different issuers and keys. The key
must no longer be selected through a global property independent of the tenant.

### Components involved

- Security configuration for `apps/user-ms`
- `TenantRegistry`
- `apps/docs/Multitenant/Step_2/JWT_Key_Resolution.md`

### Tasks

1. Link JWT key resolution to the validated tenant.
2. Configure separate keys or JWKS for AR and PNPG.
3. Verify that the `kid` values used by the tenants are distinct.
4. If needed for rollout, define a temporary fallback to
   `mp.jwt.verify.publickey`.
5. Make the fallback explicitly configurable and document its removal.
6. Reject tokens with inconsistent issuer, tenant, or `kid` values.

### Acceptance criteria

- An AR token is verified with the AR key.
- A PNPG token is verified with the PNPG key.
- A token cannot be validated using another tenant's key.
- An unknown `kid` is rejected.
- Any legacy fallback can be disabled without code changes.

### Required tests

- Valid token for AR.
- Valid token for PNPG.
- Token signed with the wrong tenant's key.
- Unknown `kid`.
- Inconsistent issuer.
- Legacy fallback enabled and disabled, if implemented.

**Dependencies:** USRMT-01, USRMT-02.

---

## USRMT-04 - Propagate the validated tenant and sanitize it in logs

**Type:** Technical story

### Objective

Propagate the already validated tenant to downstream services and prevent
untrusted values from being written to logs.

### Context

`AuthenticationPropagationHeadersFactory` must not directly forward the value
of the received header. The authoritative tenant is the normalized value stored
in `TenantContext`.

### Components involved

- New `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/TenantLogUtils.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/filter/CustomLoggingFilter.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/client/auth/AuthenticationPropagationHeadersFactory.java`

### Tasks

1. Implement sanitization of the tenant used in logs.
2. Update `CustomLoggingFilter` so that it does not log raw tenant values.
3. Modify `AuthenticationPropagationHeadersFactory` to read the tenant from
   `TenantContext`.
4. Continue propagating the authorization header to onboarding and webhook
   clients.
5. Define an explicit error when a tenant-aware call is made without a valid
   context.

### Acceptance criteria

- Logs contain only sanitized tenant identifiers.
- Onboarding and webhook calls receive the normalized tenant.
- The original tenant header is not propagated directly.
- A tenant-aware call without context is not sent with an empty or arbitrary
  value.

### Required tests

- Normalized tenant propagation.
- Authorization header retention.
- Missing `TenantContext`.
- Sanitization of control characters and invalid values.

**Dependencies:** USRMT-02.

---

## USRMT-05 - Add the tenant to the `UserInstitution` model

**Type:** Data story

### Objective

Associate each user-institution relationship with a tenant persisted in the
MongoDB document.

### Context

Separating the historical databases is no longer sufficient when a single
application serves multiple tenants. `UserInstitution` must contain the
discriminator used by all queries.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/entity/UserInstitution.java`
- Fixtures, mappers, and tests that construct `UserInstitution`

### Tasks

1. Add `tenantId` to the persisted model.
2. Update builders, fixtures, mappers, and serialization.
3. Define compatibility with historical documents that do not have `tenantId`.
4. Ensure that the field is not modified during normal entity updates.

### Acceptance criteria

- New documents contain `tenantId`.
- The value is serialized and deserialized correctly.
- An update cannot move a document from one tenant to another.
- Historical documents remain readable only through the repository's intended
  compatibility mode.

### Required tests

- Serialization and deserialization of `tenantId`.
- Construction of new entities.
- Attempt to modify the tenant of an existing entity.
- Reading a legacy document without `tenantId`.

**Dependencies:** USRMT-01.

---

## USRMT-06 - Implement a tenant-aware `UserInstitutionRepository`

**Type:** Data story

### Objective

Centralize all MongoDB operations on `UserInstitution` in a repository,
automatically applying the current tenant.

### Context

The repository must be the mandatory access point for the collection. The
logic can be adapted from `OnboardingRepository`, but it must cover all
queries, counts, writes, and bulk operations used by `user-ms`.

### Components involved

- New `apps/user-ms/src/main/java/it/pagopa/selfcare/user/repository/UserInstitutionRepository.java`
- `TenantContext`
- `selfcare.tenant.strict-data-isolation` configuration

### Tasks

1. Implement lookup by ID, query, count, insert, upsert, update, and multi-update
   operations.
2. Add the tenant predicate to every operation.
3. Automatically set the current tenant on new entities.
4. Reject entities already associated with a different tenant.
5. In compatibility mode, use the predicate:

   ```text
   tenantId = currentTenant OR tenantId is missing/null
   ```

6. In strict mode, use only:

   ```text
   tenantId = currentTenant
   ```

7. Prevent bulk operations from accepting or constructing filters that are not
   tenant-aware.

### Acceptance criteria

- Every MongoDB operation includes the tenant.
- A tenant cannot read, count, or modify another tenant's documents.
- New entities are stamped automatically.
- An entity with a different tenant is rejected.
- Compatibility mode reads legacy documents.
- Strict mode does not read documents without a tenant.

### Required tests

- Read, count, insert, upsert, update, and bulk update for AR and PNPG.
- Cross-tenant read and write.
- Automatic stamping.
- Entity with a preset, inconsistent tenant.
- Compatibility mode.
- Strict mode.

**Dependencies:** USRMT-02, USRMT-05.

---

## USRMT-07 - Remove MongoDB access that bypasses the repository

**Type:** Technical task

### Objective

Ensure that `UserInstitutionServiceDefault` accesses data exclusively through
`UserInstitutionRepository`.

### Context

Static Panache calls and direct access to `mongoCollection()` can bypass the
tenant filter, particularly for counts and bulk updates.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserInstitutionServiceDefault.java`
- `UserInstitutionRepository`
- `apps/user-ms/src/test/java/it/pagopa/selfcare/user/service/UserInstitutionServiceTest.java`

### Tasks

1. Inventory all MongoDB operations performed by the service.
2. Replace static Panache calls with repository methods.
3. Replace direct access to `mongoCollection()`.
4. Move count, upsert, and multi-update operations into the repository.
5. Preserve existing functional behavior within the same tenant.

### Acceptance criteria

- `UserInstitutionServiceDefault` contains no direct MongoDB access.
- All existing flows go through the tenant-aware repository.
- Responses for a single tenant remain compatible with the previous behavior.
- Interleaved AR and PNPG operations do not share data.

### Required tests

- Update existing service tests.
- Interleaved AR/PNPG tests for read, count, and update.
- Verify that bulk updates do not affect the other tenant.

**Dependencies:** USRMT-06.

---

## USRMT-08 - Add tenant-aware MongoDB indexes

**Type:** Infrastructure task

### Objective

Ensure that tenant-aware `UserInstitutionRepository` queries use suitable
indexes in every environment.

### Context

The current indexes do not include the tenant discriminator. The change must be
applied to the AR and PNPG stacks in DEV, UAT, and PROD without altering the
required uniqueness of `_id`.

### Components involved

- All configurations under `infra/resources/user-ms/`
- Index definitions for the `userInstitutions` collection

### Tasks

1. Add the indexes:

   ```text
   (tenantId, institutionId)
   (tenantId, userId, institutionId)
   ```

2. Evaluate whether to remove or temporarily retain indexes that are not
   qualified by tenant.
3. Apply the same configuration to DEV, UAT, and PROD for AR and PNPG.
4. Prepare a preliminary check for duplicate `_id` values before aggregating
   the datasets.
5. Document the index creation order relative to the backfill.

### Acceptance criteria

- All six stacks declare the same tenant-aware indexes.
- The repository's main queries are covered by the indexes.
- `_id` remains unique.
- The procedure reports any collisions before migration.
- The indexes are compatible with both compatibility mode and strict mode.

### Required checks

- Execution plan for the main queries.
- Terraform plan for each environment/tenant combination.
- `_id` collision report.

**Dependencies:** USRMT-05, query definitions in USRMT-06.

---

## USRMT-09 - Implement tenant-aware product storage

**Type:** Storage story

### Objective

Read `products.json` from the storage configured for the current tenant,
preventing clients, caches, or prefixes from being shared between AR and PNPG.

### Context

`user-ms` consumes the product catalog but does not require the dedicated
product database used by some `onboarding-ms` flows.

### Components involved

- New package `apps/user-ms/src/main/java/it/pagopa/selfcare/user/storage/`
- New `StorageKeys`, `PrefixingAzureBlobClient`,
  `TenantBlobClientProvider`, and `TenantAwareProductService`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/conf/UserMsConfig.java`

### Tasks

1. Implement the storage classes by adapting the behavior of `onboarding-ms`.
2. Resolve the Azure Blob client through the current tenant's `products`
   binding.
3. Isolate the client cache by tenant and logical key.
4. Apply any tenant-specific prefixes.
5. Support the storage authentication modes provided by the registry.
6. Remove the producers and fields related to flat storage from `UserMsConfig`.
7. Retain unrelated producers, including telemetry.
8. Fail explicitly when the required binding is missing or invalid.

### Acceptance criteria

- AR and PNPG can read catalogs from separate storage accounts.
- A client or cache created for AR is not reused by PNPG.
- `products.json` is resolved using the current tenant's binding.
- A missing binding or invalid credentials produce an explicit error.
- The dedicated product database logic from `onboarding-ms` is not introduced.

### Required tests

- Separate storage accounts.
- Cache isolated by tenant and logical key.
- Different prefixes.
- Missing binding.
- Missing or invalid credentials.
- Supported authentication modes.
- Interleaved AR and PNPG reads.

**Dependencies:** USRMT-01, USRMT-02.

---

## USRMT-10 - Make health checks tenant-aware

**Type:** Operations story

### Objective

Consider `user-ms` ready only when the required resources for all configured
tenants are reachable.

### Context

A check performed only on the default MongoDB client or storage is insufficient
for a shared deployment.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/health/UserMongoReadinessCheck.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/health/ProductBlobStorageReadinessCheck.java`
- Standard Quarkus health check configuration

### Tasks

1. Iterate over all tenants supported by the registry.
2. Ping each tenant's MongoDB client and database.
3. Check each tenant's required `products` storage binding.
4. Report which tenant and resource failed in detail without exposing
   credentials.
5. Disable the standard MongoDB check, which is not tenant-aware.

### Acceptance criteria

- Readiness is positive only when MongoDB and product storage are available for
  all configured tenants.
- Failure of a required resource makes the application not ready.
- The result identifies the unavailable tenant and resource type.
- No secret is returned or logged.

### Required tests

- All resources available.
- AR MongoDB unavailable.
- PNPG MongoDB unavailable.
- AR product storage unavailable.
- PNPG product storage unavailable.
- Required binding missing.

**Dependencies:** USRMT-01, USRMT-09.

---

## USRMT-11 - Add the tenant to user notification events

**Type:** Integration story

### Objective

Make the tenant an immutable part of notification events produced by
`user-ms`.

### Context

The request context is not reliably available after the asynchronous handoff.
The tenant must therefore travel in the payload.

### Components involved

- `libs/selfcare-user-sdk-event/src/main/java/it/pagopa/selfcare/user/model/UserNotificationToSend.java`
- `libs/selfcare-user-sdk-event/src/main/java/it/pagopa/selfcare/user/model/FdUserNotificationToSend.java`
- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/mapper/NotificationMapper.java`

### Tasks

1. Add `tenantId` to both event models as an additive change.
2. Map the tenant from `UserInstitution`.
3. Update serialization, fixtures, and tests.
4. Publish a compatible version of `selfcare-user-sdk-event`.
5. Identify and update consumers that must read the new field.
6. Reject the production of new events without a tenant.

### Acceptance criteria

- All new events contain `tenantId`.
- The event tenant matches the tenant of `UserInstitution`.
- The addition does not break consumers that ignore unknown fields.
- A tenant-less event is not published.

### Required tests

- Mapping AR and PNPG events.
- Serialization and deserialization.
- Payload compatibility.
- Production with a missing tenant.

**Dependencies:** USRMT-05.

---

## USRMT-12 - Send webhook notifications with the event tenant

**Type:** Integration story

### Objective

Send each webhook notification using the immutable tenant contained in the
event instead of a global property.

### Context

`user-ms.webhook.tenant-id` is not compatible with a deployment that manages
multiple tenants. The current context must not overwrite the notification's
original tenant.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/UserNotificationServiceImpl.java`
- `user-ms.webhook.tenant-id` configuration
- Models updated by USRMT-11

### Tasks

1. Remove use of the global tenant property.
2. Read the tenant from the notification payload.
3. Validate it before constructing the webhook request.
4. Propagate the validated tenant to the webhook client.
5. Reject the notification if the tenant is missing or unknown.

### Acceptance criteria

- An AR notification produces an AR webhook request.
- A PNPG notification produces a PNPG webhook request.
- The tenant in the current context cannot change the event tenant.
- Notifications without a tenant or with an unknown tenant are not sent.
- The legacy global property is no longer required.

### Required tests

- AR and PNPG webhooks.
- Event without a tenant.
- Event with an unknown tenant.
- Event and current context with different tenants.

**Dependencies:** USRMT-04, USRMT-11.

---

## USRMT-13 - Isolate the tenant in asynchronous consumers

**Type:** Security story

### Objective

Process each asynchronous callback with a dedicated tenant context that is
always released.

### Context

Threads or execution contexts may be reused. Leaving `TenantContext` populated
after a callback can cause cross-tenant access.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/internalevents/InternalEvents.java`
- `TenantRegistry`
- `TenantContext`
- CDI request context management

### Tasks

1. Read the tenant from the event payload.
2. Validate it through `TenantRegistry`.
3. Activate an isolated request context when required.
4. Set `TenantContext` before accessing a database or storage.
5. Clear the context on success, error, or retry.
6. Reject events with a missing or unknown tenant, or route them to error
   handling.
7. Verify concurrent processing of AR and PNPG events.

### Acceptance criteria

- No data access occurs before tenant validation.
- The context is always cleared.
- Concurrent callbacks from different tenants remain isolated.
- Retries and exceptions do not leave a residual tenant.
- Invalid events follow an observable error strategy.

### Required tests

- AR and PNPG callbacks.
- Missing and unknown tenant.
- Error during processing.
- Retry.
- Concurrent processing with verification that there is no context leakage.

**Dependencies:** USRMT-01, USRMT-02, USRMT-11.

---

## USRMT-14 - Make email delivery configurable per tenant

**Type:** Integration story

### Objective

Use the sender, credentials, templates, and URLs belonging to the notification
tenant, without falling back to another tenant's configuration.

### Context

Shared delivery cannot be enabled until each tenant has a complete email
configuration. Otherwise, the behavior must be fail-closed.

### Components involved

- `apps/user-ms/src/main/java/it/pagopa/selfcare/user/service/OneMailServiceImpl.java`
- Tenant registry or tenant-specific email configuration

### Tasks

1. Define for each tenant:
   - sender
   - credentials
   - template
   - dashboard URL
2. Select the configuration based on the event tenant.
3. Disable delivery for tenants with incomplete configuration.
4. Prevent fallback to another tenant's credentials.
5. Remove recipients and other personal data from logs.
6. Make terminal failures observable and apply the agreed retry policy.

### Acceptance criteria

- AR and PNPG use only their own configurations.
- An unconfigured tenant does not send email.
- There is no cross-tenant fallback.
- Logs do not contain recipients or personal data.
- Terminal failures are not ignored.

### Required tests

- Complete AR and PNPG configuration.
- Partial or missing configuration.
- Attempt to reuse another tenant's credentials.
- Retryable failure.
- Terminal failure.
- Log verification.

**Dependencies:** USRMT-11, USRMT-13, SPIKE-02.

---

## USRMT-15 - Generate the multitenant infrastructure configuration

**Type:** Infrastructure story

### Objective

Produce a consistent `user-ms` configuration for DEV, UAT, and PROD from
centralized tenant metadata.

### Context

Manually replicating the registry JSON in each stack increases the risk of
drift. The configuration must be generated from a canonical definition.

### Components involved

- `infra/resources/_modules/local-env/locals.tf`
- All stacks under `infra/resources/user-ms/`
- Container App and Key Vault module

### Tasks

1. Add `tenant_data_isolation` to the shared locals.
2. Generate the following from this data:
   - `TENANT_SUPPORTED_TENANTS`
   - `TENANT_REGISTRY_JSON`
   - references to `JWT_PUBLIC_KEY_<TENANT>`
   - references to `MONGODB_CONNECTION_STRING_<TENANT>`
   - storage bindings and identities per tenant
3. Apply the configuration to DEV, UAT, and PROD.
4. Temporarily retain the legacy variables required for a compatible
   application revision.
5. Plan their atomic removal with the application rollout.
6. Incorporate the secret management decision produced by SPIKE-01.

### Acceptance criteria

- All environments derive the registry from the same data structure.
- AR and PNPG point to independent resources.
- No secret is written in plain text in the Terraform code.
- The Terraform plan does not remove legacy variables before the compatible
  rollout.
- Secret ownership and rotation are defined.

### Required checks

- Terraform plan for DEV, UAT, and PROD.
- Check Key Vault references.
- Check managed identities.
- Test rotation of a secret through a new application revision.

**Dependencies:** USRMT-01, SPIKE-01.

---

## SPIKE-01 - Define ownership of and access to multitenant secrets

**Type:** Spike

### Objective

Choose and document how a shared Container App accesses AR and PNPG secrets.

### Context

The current Container App module assumes a single Key Vault. Before
consolidating `user-ms`, choose between a shared vault and support for multiple
vaults.

### Tasks

1. Compare:
   - shared Key Vault
   - extending the module for multiple Key Vaults
2. Check Terraform ownership and operational responsibilities.
3. Check managed identity, RBAC, and network connectivity.
4. Define the rotation procedure.
5. Document the decision, risks, and required changes.

### Completion criteria

- A single solution is approved.
- Terraform and Key Vault owners are identified.
- Permissions and connectivity are defined.
- A verifiable rotation procedure is available.
- Any additional implementation tickets are created.

**Blocks:** USRMT-15 and the shared deployment.

---

## SPIKE-02 - Define the email configuration for AR and PNPG

**Type:** Spike

### Objective

Gather the information needed to decide whether and how to enable email in a
shared deployment.

### Tasks

1. Identify the sender, credentials, template, and dashboard URL for AR and
   PNPG.
2. Identify the credential owners and rotation method.
3. Define which tenants may have email disabled.
4. Define retries, terminal errors, and observability.
5. Document any functional differences between tenants.

### Completion criteria

- A complete configuration or an explicit decision to disable email is
  available for each tenant.
- The retry policy and terminal failure handling are defined.
- No fallback between tenants is planned.
- The inputs are sufficient to implement USRMT-14.

**Blocks:** USRMT-14.

---

## DEP-01 - Make `institution-cdc` emit the actual tenant

**Type:** Cross-team story

### Objective

Ensure that events consumed by `user-ms` contain the original tenant instead
of a fixed AR value.

### Context

The current mapping in `institution-cdc` prevents PNPG events from being
correctly distinguished in a shared deployment.

### Components involved

- `apps/institution-cdc/src/main/java/it/pagopa/selfcare/institution/event/internalevents/InternalEventsMapper.java`
- Event contract consumed by `user-ms`

### Tasks

1. Identify the tenant in the event source.
2. Replace the fixed AR mapping.
3. Populate `tenantId` for AR and PNPG events.
4. Define behavior for events without a tenant.
5. Update producer, consumer, and contract tests.

### Acceptance criteria

- AR events contain the AR tenant.
- PNPG events contain the PNPG tenant.
- Events without a tenant are rejected or sent to dead letter.
- Contract tests cover both tenants.

**Blocks:** USRMT-13 for PNPG traffic and the shared deployment.

---

## USRMT-16 - Backfill tenants and verify their data

**Type:** Migration story

### Objective

Associate all existing documents with the correct tenant before enabling strict
isolation.

### Context

During the backfill, the application must already be compatible with documents
without `tenantId`. The migration must be performed separately on the AR and
PNPG datasets.

### Datasets involved

- `selcUser.userInstitutions`
- `userInfo` collection, managed separately

### Tasks

1. Verify that compatibility mode is enabled.
2. Back up the AR and PNPG datasets.
3. Search for `_id` collisions between the datasets.
4. Resolve collisions according to an approved procedure.
5. Backfill `tenantId` separately for AR and PNPG.
6. Run the script in verification mode.
7. Produce migration evidence for each environment.

### Acceptance criteria

- The backup is available and verified.
- No unresolved `_id` collisions remain.
- No document has a missing, null, unknown, or incorrect `tenantId`.
- Verification completes successfully for AR and PNPG.
- A report is available for DEV, UAT, and PROD.

### Rollback plan

- Restore from backup.
- Restore the application revision in compatibility mode.
- Do not enable strict mode until verification is successful.

**Dependencies:** USRMT-05, USRMT-06, USRMT-08.

**Blocks:** USRMT-17.

---

## USRMT-17 - Enable strict data isolation

**Type:** Security and rollout story

### Objective

Make only documents that contain exactly the current tenant accessible.

### Context

This task can begin only after the backfill has been verified. The transition
must be progressive by environment and must retain a rollback strategy.

### Tasks

1. Enable `selfcare.tenant.strict-data-isolation` in DEV.
2. Run AR/PNPG isolation tests.
3. Repeat in UAT and finally in PROD.
4. Monitor tenant-related query, count, and update errors.
5. Define rollback thresholds and criteria.
6. After the agreed period, remove the compatibility predicate for documents
   without a tenant from the code.

### Acceptance criteria

- Documents without `tenantId` are not readable.
- Cross-tenant operations neither return nor modify data.
- DEV, UAT, and PROD have passed backfill verification.
- Metrics and logs make it possible to identify tenant-specific errors.
- Compatibility mode is removed only after the observation period.

### Required tests

- Read, count, and update with the correct tenant.
- Cross-tenant read, count, and update.
- Document without a tenant.
- Rollback to the compatibility revision.

**Dependencies:** USRMT-16.

---

## USRMT-18 - Validate and release the shared deployment

**Type:** Release story

### Objective

Enable AR and PNPG traffic to a single `user-ms` deployment after verifying all
isolation guarantees.

### Prerequisites

- Tenant resolution and tenant-aware JWT are enabled.
- The repository is in strict mode.
- MongoDB and product storage are available for both tenants.
- Events, webhooks, and email are tenant-aware.
- `institution-cdc` produces the actual tenant.
- Secrets, network, and Terraform ownership are defined.

### Tasks

1. Verify startup with all required configurations.
2. Run AR and PNPG end-to-end tests for:
   - authentication
   - `UserInstitution` operations
   - product storage
   - notifications and events
   - webhooks
   - email, if enabled
3. Check MongoDB and storage readiness.
4. Verify the absence of data leakage and context leakage.
5. Test secret rotation with a new revision.
6. Define and test routing rollback.
7. Temporarily retain the legacy stacks.
8. Remove the legacy stacks only after the agreed observation period.

### Acceptance criteria

- All end-to-end tests pass for AR and PNPG.
- No cross-tenant test can access another tenant's data or configurations.
- Readiness checks all required resources.
- Secret rotation does not require application changes.
- Routing rollback has been tested.
- Removal of the legacy stacks is approved by the operational owners.

**Dependencies:** USRMT-03, USRMT-04, USRMT-07, USRMT-10, USRMT-12,
USRMT-13, USRMT-14, USRMT-15, USRMT-17, DEP-01, and SPIKE-01.

---

## Proposed delivery sequence

```text
USRMT-01
  -> USRMT-02
  -> USRMT-03 / USRMT-04 / USRMT-05 / USRMT-09
  -> USRMT-06 / USRMT-08 / USRMT-11
  -> USRMT-07 / USRMT-10 / USRMT-12 / USRMT-13
  -> SPIKE-01 / SPIKE-02 / DEP-01
  -> USRMT-14 / USRMT-15
  -> USRMT-16
  -> USRMT-17
  -> USRMT-18
```

The spikes and the `DEP-01` dependency must be started early enough not to
block integration and the final release.
