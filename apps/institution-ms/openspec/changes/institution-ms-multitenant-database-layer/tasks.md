# Tasks

## 1. Shared Spring Integration and Configuration

- [x] 1.1 Wire the configured `MongoConverter` into the shared `TenantMongoConfiguration` template bean while keeping one primary routed template; add Spring configuration tests proving custom conversions are honored and standard mapping still works.
- [x] 1.2 Align `apps/institution-ms` commons dependency management to the compatible tenant-enabled release and declare `selc-commons-tenant` in modules using its types; verify the affected Maven modules compile and their dependency trees contain one aligned commons family.
- [x] 1.3 Import shared tenant/Mongo configurations in `SelfCareCoreApplication`, exclude `MongoAutoConfiguration`, and retain existing mapping/repository and async configuration; add a context test verifying repositories, `MongoTemplate`, and `MongoOperations` use the routed factory without duplicate beans.
- [x] 1.4 Add DAO/application tenant settings, the single-tenant registry default, and environment-shaped connection/key aliases preserving existing institution variables and URI options; test resolved defaults, explicit two-tenant configuration, allowlisting, malformed references, and missing bindings/connection values.
- [x] 1.5 Ensure every supported tenant has a resolvable JWT key binding before startup using a narrow service check for absent definitions; add startup tests for missing definitions and missing/blank key values, and confirm failures expose no key or connection values.
- [x] 1.6 Add ordered tenant-context hooks and test configuration to the institution Cucumber setup, following `user-group-ms`; verify fixture access selects the configured tenant and context is cleared after database cleanup on both success and failure.
- [x] 1.7 Document the registry shape, environment aliases, tenant-header requirement, supported/default tenant controls, and enabled-only Mongo routing mode in `apps/institution-ms/README.md`; confirm documented examples pass the same configuration tests.

## 2. Document Ownership and Shared DAO Policy

- [x] 2.1 Add internal tenant fields and field-name constants to institution, delegation, and mail-notification entities without extending public models; update Java fixtures and Compose seed data, and test persisted markers plus unchanged API serialization.
- [x] 2.2 Add one reusable DAO ownership-policy helper for strict and explicit legacy-compatible criteria; test foreign, missing, null, and matching markers, missing context, and safe composition with existing `$or` filters.
- [x] 2.3 Apply the helper to all query-based `MongoCustomConnectorImpl` operations while preserving projections, sort, pagination, and query options; expand `MongoCustomConnectorImplTest` to cover find, exists, count, paginated find, update, removal, and upsert query shapes.
- [x] 2.4 Add an atomic ownership-guarded entity replacement/insert path with server-assigned tenant metadata; test successful own-tenant replacement, new insertion, and rejection of conflicting foreign ownership without an unguarded read-then-save window.
- [x] 2.5 Add a routed mapping round-trip test for institution and delegation `OffsetDateTime` values, including non-UTC offsets; assert existing BSON string representation and recovered values through the actual configured template.
- [x] 2.6 Document strict-default visibility and compatibility semantics next to the configuration instructions; confirm the text explicitly excludes foreign markers, limits compatibility to the selected database, and does not promise automatic legacy backfill.

## 3. Institution Persistence

- [x] 3.1 Replace unscoped institution inherited repository calls for list, ID/bulk-ID read, and deletion with ownership-scoped operations; extend `InstitutionConnectorImplTest` with strict/compatibility cases and retain existing not-found behavior.
- [x] 3.2 Route institution saves through the guarded write path, assigning tenant metadata after mapping and preventing ownership conflicts from triggering the existing UUID-retry success path; test own-tenant save, legacy replacement, foreign-ID conflict, and existing valid create behavior.
- [x] 3.3 Scope the annotated onboarding projection and all onboarding, geography, product, origin, tax-code, existence, and nested-update paths; add regression assertions for the product projection, array filters, timestamps, and exclusion of foreign documents.

## 4. Delegation Persistence and Aggregations

- [x] 4.1 Tag delegation saves and replace or scope both derived repository queries; extend `DelegationConnectorImplTest` to cover tenant-specific existence checks, status mutations, activation, and bulk institution-reference updates.
- [x] 4.2 Scope direct template counts, date-based facets, and grouped statistics before counting or paging; test mixed-marker datasets so result totals, groups, sort order, and pages include only eligible documents in both isolation modes.
- [x] 4.3 Scope delegation-to-institution lookups independently on root and joined documents before page selection can be affected; test a foreign joined institution, a later eligible result after an excluded join, and unchanged no-match/unwind semantics.
- [x] 4.4 Scope the custom user-institution aggregation source and `$graphLookup` traversal while retaining binding, product-state, external-ID, and depth filters; test owned sources with foreign and legacy targets under both isolation modes.

## 5. Mail-Notification Persistence

- [x] 5.1 Scope notification upsert, product removal, and empty-document cleanup and assign tenant metadata on insert; expand `MailNotificationConnectorImplTest` for own, foreign, missing/null marker, and no-match cases without changing existing return semantics.
- [x] 5.2 Add database-backed notification scenarios covering insert-through-upsert, last-product deletion, and foreign-document preservation; verify no successful new notification is untagged in either strict or compatibility mode.

## 6. Database Readiness

- [x] 6.1 Update `MongoReadinessIndicator` to probe supported tenants using scoped context and disable the default Mongo health contributor; extend its tests for all-UP, one-DOWN, allowlist exclusion, and context restoration with and without a prior tenant.
- [x] 6.2 Exercise the actual readiness endpoint without a tenant header; verify stable component names, DOWN on a supported-tenant ping failure, sanitized diagnostics, and unchanged inclusion of both blob-storage readiness components.
- [x] 6.3 Document that readiness covers every supported database and that registry/client changes require restart; verify operational examples refer to the existing readiness endpoint and component names.

## 7. Cross-Layer Acceptance and Cutover Guidance

- [x] 7.1 Extend the existing Spring Boot/Cucumber suite with two tenant databases and independently authenticated HTTP requests; verify same-ID institution isolation and tenant-specific delegation/mail changes, and retain existing single-tenant feature behavior.
- [x] 7.2 Add request-level regression scenarios for normalized valid headers, unsupported/duplicate/missing headers, JWT tenant mismatch, and context cleanup after request failures; verify the shared authentication contract's 401 responses for missing/unsupported tenants and 400 problem responses for duplicate/mismatched headers, with no domain persistence.
- [x] 7.3 Exercise strict and compatibility modes against explicitly seeded owned, foreign, absent-marker, and null-marker documents; verify CRUD, totals, nested updates, lookup/graph traversal, and new-write tagging satisfy the capability scenarios.
- [x] 7.4 Complete README rollout and rollback guidance for producer coordination, reviewed legacy backfill, a single-owner compatibility window, strict cutover, and stopping multi-tenant traffic before rollback; verify it promises neither automated production migration nor shared-database tenant coexistence.
- [x] 7.5 Run the existing targeted shared-tenant, DAO, health, and affected web tests in batched runner invocations, then the affected institution Cucumber scenarios with Docker; prefer resolved Nx targets when available, otherwise use existing Maven module selectors, and confirm no public schema, date-format, projection, or readiness regressions.

## 8. Terraform Deployment Wiring

- [x] 8.1 Update `infra/resources/institution-ms/{dev,uat,prod}-{ar,pnpg}/main.tf` following the corresponding `user-group-ms` roots: explicitly set supported/default tenant (`AR` or `PNPG`), enabled Mongo routing, and strict isolation; encode the domain-only registry with `jsonencode`, the existing `module.cosmosdb.database_name`, tenant-specific MongoDB/JWT reference names, and empty storage definitions. Verify all six deployments retain their original tenant/account/database and do not enable another tenant.
- [x] 8.2 Wire `MONGODB_CONNECTION_STRING_AR`/`JWT_PUBLIC_KEY_AR` or `MONGODB_CONNECTION_STRING_PNPG`/`JWT_PUBLIC_KEY_PNPG` to the existing deployment's `mongodb-connection-string`/`jwt-public-key` Key Vault secrets; replace legacy secret-map environment keys without duplicate Container App secret declarations and retain documented legacy local configuration. Add optional `tenant.mongodb.application-name` support to the shared tenant Mongo client configuration; set it in institution DAO configuration from `${MONGODB_APPNAME:selcMsCore}` and test that the override preserves URI options while an absent value retains URI application-name behavior. Verify the effective connection options and appname without exposing credentials or modifying vault values.
- [x] 8.3 Update `apps/institution-ms/README.md` with the six Terraform roots, domain allowlist/default and secret-reference mapping, strict-baseline data-preparation prerequisite, explicitly authorized compatibility override, coordinated application/configuration release, readiness/smoke checks, and rollback restoring the previous application plus legacy environment names. Confirm the guidance does not promise new provisioning, automatic backfill, or shared-database tenant coexistence.
- [x] 8.4 Run existing targeted Terraform formatting/validation tooling for all six affected roots and review the source diff only; do not run `terraform plan` or `terraform apply`. Verify every registry reference has the intended secret mapping, registry JSON contains no credentials, and changes are limited to tenant application settings and secret references with no database/collection/index/lock/state-address, networking, identity, storage, scaling, or unrelated service changes.
