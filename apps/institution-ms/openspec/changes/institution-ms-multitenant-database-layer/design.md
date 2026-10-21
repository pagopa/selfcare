# Design

## Context

See `proposal.md` for motivation and `specs/institution-tenant-persistence/spec.md` for the behavior contract.

The target is a multi-module Spring Boot service, not a Quarkus application:

| Surface | Observed implementation |
| --- | --- |
| Bootstrap | `apps/institution-ms/app/.../SelfCareCoreApplication.java` enables normal Mongo auto-configuration and scans the service packages. |
| Binding | `connector/dao/src/main/resources/config/dao-config.properties` configures one URI and database through `MONGODB_CONNECTION_URI`, `MONGODB_APPNAME`, and `MONGODB_NAME_SELC_MSCORE`. |
| Persistence | `InstitutionRepository`, `DelegationRepository`, and `MailNotificationRepository` combine `MongoRepository` with `MongoCustomConnector`. `DelegationConnectorImpl` also injects `MongoTemplate` directly. |
| Bypasses | Institution CRUD uses inherited ID and list methods; onboarding has an annotated projection query; delegation checks use derived queries. These bypass the custom query fragment. |
| Aggregations | Delegation pagination, statistics, and institution lookups use the template; the custom fragment also supports `$graphLookup` over configurable source/target collections. |
| Mapping | `DaoConfig` registers `OffsetDateTime` string converters. The three persisted entities currently have no tenant field. |
| Health | `MongoReadinessIndicator` pings one database and is included as `mongoReadiness` alongside existing blob checks. |
| Reference | `apps/user-group-ms` imports shared tenant and Mongo configuration, stores `tenantId`, and scopes queries with strict or explicit legacy-compatible criteria. |
| Deployment | `infra/resources/institution-ms` has six DEV/UAT/PROD AR/PNPG roots using legacy MongoDB/JWT environment names without explicit tenant settings. The corresponding `user-group-ms` roots already supply a tenant allowlist/default, registry, and tenant-specific secret references. |

The reusable Spring implementation is in `libs/selfcare-commons/tenant`, not the similarly named Quarkus SDKs. Its factory routes every database access through `TenantContext`, caches per-tenant clients, and closes them on destruction. Its registry validates secret-reference names as environment-variable-style identifiers. Its current template bean constructs `new MongoTemplate(factory)` without the application's configured converter.

`institution-ms` currently consumes commons `2.10.0`; `user-group-ms` declares `2.20.0`, while the in-repository commons sources declare `2.21.0`. Copy the reference architecture, not its pinned version or its dotted secret-reference defaults, which conflict with the current registry validator.

## Goals / Non-Goals

**Goals:** Keep a single routed Spring persistence graph; make every application DAO path obey the same ownership policy; retain institution-specific mapping and public contracts; provide a controlled path from untagged existing data; wire the existing Terraform deployments to the tenant registry without reprovisioning their databases.

**Non-Goals:** Do not introduce tenant selection in business DTOs, globally intercept arbitrary Mongo queries, add a second tenant SDK, migrate frameworks, support multiple logical tenants sharing a production database, or extend tenant routing to non-database resources. Database isolation is not a claim of complete service-wide resource isolation.

## Decisions

### 1. Reuse the Spring commons tenancy graph

Add an explicit `selc-commons-tenant` dependency wherever compile-time types are used, manage it centrally with the other commons artifacts, and align the service's commons family to a compatible release containing the inspected implementation. Start from the current in-repository release line; publish a new commons version if the mapping change below requires it rather than modifying an already published artifact.

Import `TenantConfiguration` and `TenantMongoConfiguration` in `SelfCareCoreApplication`, and exclude `MongoAutoConfiguration`, as `user-group-ms` does. Retain Spring Data's mapping/repository auto-configuration and existing async configuration. Do not import `TenantStorageConfiguration` for this database-only change.

Reuse the tenant-aware commons `SecurityConfig` already imported through `CoreSecurityConfig`. Its authentication filter establishes context from the authenticated token and clears it in `finally`. JWT strategies validate the normalized header against the verified tenant claim and their existing default rules. Do not add a parallel header-only filter or change those authentication rules.

**Alternative rejected:** a local connection router would duplicate existing pooling, registry validation, authentication integration, and lifecycle management.

### 2. Use registry bindings and strict ownership by default

Add the reference settings to the existing DAO/application property sources: `tenant.mongodb.enabled`, `tenant.supported-tenants`, `tenant.default`, `tenant.strict-data-isolation`, and `tenant.registry.json`, mapped to `TENANT_MONGODB_ENABLED`, `TENANT_SUPPORTED_TENANTS`, `TENANT_DEFAULT`, `TENANT_STRICT_DATA_ISOLATION`, and `SELFCARE_TENANT_DATA_ISOLATION`. Default the supported/default tenant to `AR` and strict isolation to `true`, matching `user-group-ms`. Keep the shared PagoPA strategy's existing default semantics.

The single-tenant default registry points to the existing institution database name and effective URI, including existing connection options. Provide environment-shaped Spring property aliases, for example `INSTITUTION_MONGODB_CONNECTION` resolving from `spring.data.mongodb.uri` and `INSTITUTION_JWT_PUBLIC_KEY` resolving from `jwt.signingKey`. Reference those aliases from the default registry; do not copy the reference service's `spring.data.mongodb.uri` or `jwt.signingKey` as registry reference names. Alias values remain outside the JSON registry.

A multi-tenant registry uses externally supplied reference names, for example:

```json
{
  "AR": {
    "mongo": {
      "account": "institution-ar",
      "database": "institution-ar",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    },
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR" },
    "storages": {}
  },
  "PNPG": {
    "mongo": {
      "account": "institution-pnpg",
      "database": "institution-pnpg",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG"
    },
    "jwt": { "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG" },
    "storages": {}
  }
}
```

Require a JWT key binding for every supported tenant in the documented service configuration. The registry validates supplied references; add a narrow service configuration check for absent JWT definitions if the shared registry does not enforce their presence. Once a registry is active, `JwtService` requires a tenant key rather than using the old global-key fallback. The local alias preserves existing `JWT_TOKEN_PUBLIC_KEY` configuration through `jwt.signingKey`.

`TENANT_MONGODB_ENABLED=true` is the supported operating mode for this change. Do not promise that setting it to `false` silently restores the legacy connection; the shared bean is conditional and Mongo client auto-configuration is excluded.

**Alternative rejected:** silent fallback on invalid registries or missing context could select the wrong database. Empty storage definitions do not activate new storage dependencies or mandatory storage keys.

### 3. Preserve the configured Mongo mapping converter

Extend the existing shared `TenantMongoConfiguration` template bean to consume the configured `MongoConverter` instead of constructing its own default converter. Spring Boot's retained mapping configuration supplies the converter backed by the existing `MongoCustomConversions` and mapping context. Keep one primary routed template, usable through both `MongoTemplate` and `MongoOperations`.

This small shared integration change is necessary: simply importing the current bean can bypass `DaoConfig`'s date conversions. Add focused shared-configuration and institution round-trip regression coverage, including an application with standard conversions, rather than creating competing application-local template beans.

**Alternative rejected:** duplicating the tenant configuration in `institution-ms` or registering another primary template introduces ambiguous beans and divergent behavior.

### 4. Centralize ownership predicates at the DAO boundary

Add internal `tenantId` fields to `InstitutionEntity`, `DelegationEntity`, and `MailNotificationEntity`, using their existing field-name conventions. Do not extend domain models or request/response DTOs. Set ownership after mapping from domain data.

Introduce one DAO ownership-policy helper using `TenantContext.requiredTenantId()` and `tenant.strict-data-isolation`. It produces:

- Strict: `tenantId = currentTenant`.
- Compatibility: `tenantId = currentTenant OR tenantId = null`, where Mongo null equality also matches absent fields.

Combine that predicate with domain criteria using a top-level conjunction so existing `$or` filters cannot override it. Preserve projection, sort, pagination, collation, and query options. Build fresh criteria per operation; do not cache mutable queries across requests.

Apply the helper in `MongoCustomConnectorImpl` for its query-based methods, but also replace or explicitly scope inherited/derived repository calls in the three connectors. In particular cover institution `findAll`, `findAllById`, `findById`, `deleteById`, the onboarding projection, and both delegation derived queries. Direct template counts and queries require the same policy. Repository interfaces alone are not an isolation guarantee.

Use atomic tenant-scoped replacement/upsert operations for entity saves rather than an unqualified ID-based `repository.save`. New inserts and upsert insert branches assign the current tenant. If an existing ID belongs to a foreign tenant, the ownership predicate must prevent replacement; a uniqueness conflict must surface instead of triggering the institution connector's current UUID-retry path and returning success under a different ID. Avoid read-then-save ownership checks with an unguarded final write.

Partial updates of legacy documents in compatibility mode do not perform a blanket backfill. Replacement writes and new inserts are tagged; explicit operational backfill is still required before strict cutover.

**Alternative rejected:** database routing alone would not match `user-group-ms`'s document policy or protect against misfiled foreign documents. Adding predicates only to the custom fragment misses several current entry points.

### 5. Scope aggregation roots and joins independently

Add ownership matches before delegation counts, facets, sorting/limiting, and grouping in `findFromDate`, `findAndCount`, `findDelegators`, `findDelegates`, and `countDelegations`.

For institution lookups, use an ownership-filtered lookup pipeline, or an equivalent foreign-document filter before a join can influence page selection. Avoid applying a post-limit filter that makes a page short when later eligible results exist. Preserve current unwinding and no-matching-institution semantics.

For `findUserInstitutionAggregation`, apply the active policy to both the source and `$graphLookup.restrictSearchWithMatch`, combining it with the existing external-ID restriction. Preserve existing binding/product filters and traversal depth. Any legacy source collection used by this path needs the same ownership preparation; do not weaken strict mode because a collection was not part of the three entity classes.

**Alternative rejected:** filtering only the source delegation or user binding can expose another tenant's institution through a join within the selected database.

### 6. Keep readiness context-independent and tenant-complete

Adapt `MongoReadinessIndicator` using the `user-group-ms` pattern: iterate `TenantRegistry.supportedTenantIds()` and run each ping inside `TenantContext.withTenant(...)`. Preserve `mongoReadiness`, `mongodb-institution`, and the existing shared health response contract. A failed supported-tenant probe causes DOWN; no unsupported entry is probed. The shared helper restores context even on exceptions.

Disable Spring Boot's default Mongo health contributor, as the reference service does, so it cannot invoke the routed template without a context. Do not change blob-storage checks or claim that legacy single-database host metadata represents every tenant. Keep diagnostics sanitized.

### 7. Exercise both routing and document guards

Use the existing JUnit/Mockito and Spring Boot/Cucumber infrastructure, not a new runner. Two distinct databases prove binding isolation; foreign-marked and legacy documents deliberately seeded into one selected database prove ownership predicates. Different connections in shared factory coverage prove account selection without requiring production resources.

Add explicit tenant setup/cleanup hooks, following `user-group-ms`'s `TenantContextHooks`, for direct fixture repository/template access. Order cleanup so the context is cleared after database cleanup. Mark ordinary fixtures with their tenant; keep intentional untagged fixtures only in compatibility scenarios. Adapt Compose initialization data as well as Java-built fixtures.

HTTP scenarios must send tenant headers and matching JWTs independently of fixture-thread context. Add failure-path and worker-reuse coverage. Exercise complete routed converter wiring, nested onboarding updates, annotated projections, aggregation joins/counts, bulk changes, atomic ownership-safe saves, and mail upsert/removal.

Prefer existing Nx targets after inspecting their resolved configuration. Planning discovery could not query Nx because `pnpm` is absent in this environment; no Nx project/target names are assumed here. During implementation, use the existing Maven module selectors when no usable Nx target is available, with configured private-repository access and Docker for Cucumber.

### 8. Wire existing Terraform deployments to the tenant registry

Update `main.tf` in all six roots under `infra/resources/institution-ms`: `dev-ar`, `uat-ar`, `prod-ar`, `dev-pnpg`, `uat-pnpg`, and `prod-pnpg`. Follow the corresponding `infra/resources/user-group-ms` roots' application-setting and Key Vault reference pattern, substituting the institution database rather than copying `selcUserGroup`.

Each existing deployment remains single-tenant even though the application supports multiple bindings:

| Deployment domain | Supported/default tenant | Registry account label | MongoDB reference | JWT reference |
| --- | --- | --- | --- | --- |
| AR | `AR` | `cosmos-ar` | `MONGODB_CONNECTION_STRING_AR` | `JWT_PUBLIC_KEY_AR` |
| PNPG | `PNPG` | `cosmos-pnpg` | `MONGODB_CONNECTION_STRING_PNPG` | `JWT_PUBLIC_KEY_PNPG` |

Set `TENANT_SUPPORTED_TENANTS` and `TENANT_DEFAULT` explicitly for that domain, `TENANT_MONGODB_ENABLED=true`, and `TENANT_STRICT_DATA_ISOLATION=true`. Supply `SELFCARE_TENANT_DATA_ISOLATION` with Terraform `jsonencode`, containing only the deployment's tenant, its `mongo` and `jwt` definitions, and `storages: {}`. Use the existing `module.cosmosdb.database_name` (`selcMsCore`) as the Mongo database. Account labels identify the bindings; they do not provision or select a different Cosmos DB account independently of the connection.

Map the tenant-specific environment references to the existing `mongodb-connection-string` and `jwt-public-key` secrets in the deployment's existing Key Vault. Do not put connection strings or key values in registry JSON or ordinary Terraform application settings. Replace the legacy secret-map environment keys rather than adding duplicate mappings to the same vault secret: `container_app_microservice` currently emits one Container App secret declaration per map value without deduplication. Keep the documented legacy application configuration available for local/default single-tenant use and for rollback.

Preserve the effective MongoDB connection options when moving from `INSTITUTION_MONGODB_CONNECTION` (which resolves the legacy effective URI) to a tenant-specific reference. Verify that the configured tenant connection retains the existing URI options and application name; if the raw vault URI lacks an option added by application configuration, retain that composition through configuration rather than exposing credentials in Terraform. Do not silently route to a localhost default or change a vault secret's value as part of this wiring.

The raw tenant connection reference is consumed by `TenantMongoDatabaseFactory` rather than by Spring Boot's `spring.data.mongodb.uri`, so URI composition in `dao-config.properties` does not apply to it. Preserve the existing `MONGODB_APPNAME` behavior by adding an optional `tenant.mongodb.application-name` configuration value, mapped in `institution-ms` to `${MONGODB_APPNAME:selcMsCore}`. The shared factory applies it to `MongoClientSettings` after parsing each tenant's `ConnectionString`; this retains all URI-provided options while setting the same effective application name without exposing or rewriting the secret. When the property is absent/blank, leave the connection string's own application-name setting unchanged, preserving behavior for other consumers. Cover both cases and option retention with focused shared-library tests.

Leave database and collection resources, indexes, locks, Terraform state addresses, container names, scaling, networking, managed identities, storage, mail, telemetry, and downstream-service settings unchanged. Do not add both tenants to a deployment without a separate operational decision and prepared bindings. Update the institution README with the six-root mapping, secret-reference contract, and rollout/rollback requirements when implementing this delta.

Use existing Terraform formatting and validation tooling on the affected roots, then review the source diff for application-setting and secret-reference changes only. Do not run `terraform plan` or `terraform apply`, and do not migrate production data as part of implementation. Verify from the source that every registry reference matches the intended environment-specific Key Vault secret and that each deployment retains its original account and database.

**Alternative rejected:** relying on the application's default `AR` registry would misconfigure PNPG deployments. Provisioning new databases or copying the user-group database name would redirect existing institution data rather than enable routing safely.

## Risks / Trade-offs

- [Strict defaults hide untagged records] -> Backfill before cutover, or explicitly enable a short single-owner compatibility window; never silently treat all legacy data as owned.
- [Commons upgrade changes authentication behavior] -> Align the commons family, preserve shared strategy rules, supply tenant JWT key bindings, and cover existing valid requests plus tenant failures.
- [Compatibility mode cannot identify the owner of untagged records] -> Use it only when the selected database has one known legacy owner; never use it to mix tenants in one database.
- [Existing unique indexes and `_id` are database-wide] -> Keep distinct tenant database bindings. Do not promise same-ID or same-external-ID coexistence in a shared production database or change shard/index design in this change.
- [Counts, joins, and replacement writes can bypass ordinary filtering] -> Cover each identified path explicitly, including conflicting ownership in the selected database.
- [Custom mapping can be lost behind the shared template] -> Wire the configured converter and assert both BSON representation and offset-aware round trips.
- [A request context is thread-local] -> Fail closed for context-free domain access; use explicit scoped context for health and test setup. No new asynchronous domain workflow is introduced.
- [Registry changes require restarting cached clients] -> Document restart-based rollout and reuse the existing factory shutdown lifecycle rather than add live reload.
- [External collection writers may continue inserting untagged data] -> Coordinate the strict cutover with those producers; compatibility is temporary and does not fix their writes.
- [PNPG deployments inherit the application default `AR`] -> Configure the supported/default tenant and registry explicitly in every Terraform root.
- [Tenant secret renaming drops URI options or duplicates Container App secrets] -> Preserve effective connection options and replace environment-map keys without duplicate secret declarations; inspect the source diff and run local formatting/validation only.

## Migration Plan

1. Prepare the six existing Terraform roots with domain-specific tenant settings and references to their existing Key Vault secrets and `selcMsCore` databases. Additional tenants require distinct prepared database bindings; provisioning new resources is outside this implementation. Inventory legacy ownership, including `Institution`, `Delegations`, `MailNotification`, and any source collections used by graph aggregation.
2. Coordinate other writers/readers of these collections. Keep the rollout single-tenant until the relevant writers persist tenant metadata or the legacy window has an explicit owner.
3. Backfill `tenantId` using a reviewed, tenant-specific operational procedure, or explicitly authorize a temporary `TENANT_STRICT_DATA_ISOLATION=false` override for the existing tenant's known database. The Terraform baseline remains strict; production backfill execution and new resource provisioning are outside this implementation.
4. Through the normal deployment process, release the aligned commons dependencies and service together with its matching Terraform application settings and secret references. Verify all configured databases are ready and run tenant-scoped CRUD/join smoke checks; preserve existing storage settings. Do not apply infrastructure during the implementation workflow.
5. After backfill and producer coordination, enable strict mode and restart. Add the next tenant only with its own prepared binding and matching authentication configuration.
6. Rollback by stopping multi-tenant traffic first, then restoring the previous application and its original single-database Terraform settings and legacy MongoDB/JWT environment names together. The old application does not enforce tenant markers; never point it at mixed-tenant data or keep serving the new tenant. Leave additive tenant metadata in place and do not drop data or indexes.
