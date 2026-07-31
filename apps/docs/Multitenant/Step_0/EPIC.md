# Jira Epic — Multitenant Architecture Rollout

Source inputs: `REQUIREMENTS.md`, `ARCHITECTURE.md`, `SECURITY.md` (this folder).

## Epic

**Title:** Multitenant backend consolidation (`selfcare.pagopa.it` + `imprese.notifichedigitali.it`)

**Description:**
Refactor the Selfcare microservices so a single backend deployment per environment tier serves both tenants
(`selfcare.pagopa.it` / AR, `imprese.notifichedigitali.it` / PNPG) instead of maintaining separate `-ar`/`-pnpg`
deployments. Tenant identity MUST be resolvable from both an `X-Tenant-Id` HTTP header (injected/overwritten by
APIM from the `Host` header) and a JWT claim (issued by `auth` for OneIdentity, and injected for
`hub-spid-login`, defaulting to `PNPG` when absent). Rollout follows a parallel-run migration: new shared stack
deployed alongside legacy stacks, cutover at the APIM layer, legacy decommissioned only after production
validation.

**Goal / business value:** Eliminate duplicated per-tenant deployments and operational overhead while
preserving strict tenant isolation and existing security posture for both authentication flows.

**Primary requirement groups covered:** SELC-1 through SELC-7 (`REQUIREMENTS.md`).

**Definition of Done (epic-level):**
- Both tenant hostnames resolve through one shared backend deployment per environment tier.
- Every request carries a validated, consistent tenant identity (header + claim) end-to-end, with no
  cross-tenant data leakage and no silent tenant fallback (except the documented `hub-spid-login` → `PNPG`
  default).
- Legacy `-ar`/`-pnpg` Terraform stacks, secrets, and certificates are decommissioned after validation.
- Still-open items (claim name/format, `hub-spid-login` injection mechanism, final per-service isolation model,
  RBAC/ABAC model, rate-limit thresholds) are resolved or explicitly descoped before epic closure.

**Out of scope:** Frontend (React) application changes beyond removing any client-side `X-Tenant-Id` usage;
fine-grained authorization model design (tracked separately, currently `TO BE DECIDED`).

---

## Sub-tasks

### 1. Define tenant-resolution strategy and canonical tenant registry
- **Maps to:** SELC-6, SELC-5.4, SELC-6.3
- **Description:** Decide and document the tenant claim name/format, the canonical list of tenant IDs
  (`default`/AR, `PNPG`), and the host→tenant mapping source of truth (Terraform-managed APIM Named Values or
  global policy map). This is a prerequisite for all other sub-tasks.
- **Acceptance criteria:** Single documented mapping consumable by APIM, `auth`, and the `hub-spid-login`
  injection layer; extensible to a new tenant without code changes (SELC-6.2).
- **Blockers/open questions:** Claim name/format still `TO BE DECIDED`.

### 2. APIM: resolve tenant from `Host` header and enforce `X-Tenant-Id`
- **Maps to:** SELC-1, SELC-5
- **Description:** Implement APIM policy that resolves tenant from the `Host` header using the registry from
  sub-task 1, and unconditionally overwrites any client-supplied `X-Tenant-Id` before forwarding to the shared
  backend (anti-spoofing, per `SECURITY.md` HTTP boundary rules).
- **Acceptance criteria:** Requests to either tenant hostname reach the shared backend with a correct, non
  client-controlled `X-Tenant-Id`; requests with unknown hosts are rejected, not defaulted.
- **Depends on:** Sub-task 1.
- **Status: implemented (within current per-tenant deployment topology).** The shared `apim_api` Terraform
  module (`infra/resources/_modules/apim_api`) now takes a required `tenant_ids = list(object({ id, origin }))`
  variable declaring every tenant frontend an API group serves (e.g.
  `[{ id = "AR", origin = "https://selfcare.pagopa.it" }, { id = "PNPG", origin = "https://imprese.notifichedigitali.it" }]`).
  Its inbound policy builds a CORS allow-list from that list and resolves `X-Tenant-Id` at request time from the
  calling `Origin`/`Referer` against it, unconditionally overriding any client-supplied value (never trusted from
  the caller); an origin that matches none of the declared tenants is rejected with `403 application/problem+json`
  (fail-closed), and requests without `Origin`/`Referer` (server-to-server, health checks) fall back to
  `tenant_ids[0]`. All 27 call sites (`dashboard-bff`, `onboarding-bff`, `iam`, `auth`, `webhook`,
  `registry-proxy`; every `dev`/`uat`/`prod` × `-ar` deployment folder that exists today, since the `-pnpg`
  folders are slated for deprecation once a single `-ar` deployment per environment serves both tenants) already
  declare **both** `AR` and `PNPG` in `tenant_ids`, so every API group is ready to accept and correctly label
  traffic from either frontend ahead of that consolidation. The `tenant_ids` origin table is centralized once in
  `infra/resources/_modules/local-env` (`local.tenant_frontend_origins`, keyed by `env`) and exposed as
  `module.local.config.tenant_ids` — every call site simply sets `tenant_ids = module.local.config.tenant_ids`,
  eliminating per-file duplication of the AR/PNPG origin list; the module orders its own tenant first
  (`local.domain`-aware) so the no-`Origin` fallback (`tenant_ids[0]`) still resolves correctly per deployment.
  True `Host`-header-based resolution (a single API definition backed by a single shared Container App) is
  completed by this policy already for the origin resolution/label; only the backend consolidation itself (one
  Container App instead of `-ar`/`-pnpg` pairs) is deferred to sub-task 7.

### 3. `auth` microservice: tenant claim issuance (OneIdentity flow)
- **Maps to:** SELC-4
- **Description:** Extend `auth` to add the tenant claim to JWTs it issues, using the same resolution strategy
  as APIM (sub-task 1).
- **Acceptance criteria:** JWTs issued via OneIdentity login contain the tenant claim, consistent with the
  `X-Tenant-Id` the same client would receive from APIM.
- **Depends on:** Sub-task 1.
- **Status: implemented.** Tenant is resolved fresh from the incoming `X-Tenant-Id` header (via a shared
  `TenantHeaderUtils.resolveTenantId`, reusing `TenantId`/`TenantConstants` from `libs/selfcare-sdk-security`)
  at each of the three OneIdentity entry points — `SamlCallbackController`, `OidcController`, `OtpController`
  (`/verify` only) — and threaded through `UserClaims.tenantId` into `SessionServiceImpl`, which embeds it as
  the `tenant_id` claim on every issued JWT (both `generateSessionToken` and `generateSessionTokenInternal`).
  Resolution fails closed with a 400 (`InvalidRequestException`) if the header is missing/unknown; token
  signing fails closed with a 500 if `tenantId` is somehow absent at that point (defense in depth). 167 unit
  tests pass in `apps/auth` (no regressions).

### 4. `hub-spid-login`: tenant claim injection layer
- **Maps to:** SELC-3
- **Description:** Design and implement the mechanism to inject the tenant claim into `hub-spid-login` tokens
  (post-processing, wrapper, or shared issuer — mechanism `TO BE DECIDED`). Validation MUST default to `PNPG`
  when the claim is absent (SELC-3.1), and the `X-Tenant-Id` header MUST still be present and equal to `PNPG`
  in that case (SELC-3.4).
- **Acceptance criteria:** Tokens from `hub-spid-login` are either claim-bearing or correctly defaulted; SPID
  assertion validation is not weakened by the new injection point (`SECURITY.md` Authentication rules).
- **Depends on:** Sub-task 1. **Blockers:** injection mechanism not yet decided (Open Question in
  `REQUIREMENTS.md`).

### 5. Backend services: tenant header/claim enforcement filter
- **Maps to:** SELC-1.2/1.3, SELC-2, SELC-7.1
- **Description:** Add a cross-cutting check (per service or shared filter) that validates `X-Tenant-Id`
  presence/known-tenant match, reconciles it against the JWT tenant claim, applies the `hub-spid-login`
  default-to-`PNPG` exception, and hard-rejects on any other mismatch or missing header.
- **Acceptance criteria:** All authenticated endpoints reject tenant-inconsistent requests; no endpoint
  authorizes on the header alone.
- **Depends on:** Sub-tasks 2, 3, 4.
- **Status: wired into `product` (proof-of-concept microservice) and verified end-to-end.** Filter itself
  remains in `libs/selfcare-sdk-security` (tenant package), unit-tested (14 tests). The lib ships no
  Jandex index/`beans.xml`, so `TenantValidationFilter`/`TenantContext` are not CDI-discoverable by a
  consumer without indexing the dependency; added
  `quarkus.index-dependency.selfcare-sdk-security.{group,artifact}-id` to `apps/product`'s
  `application.properties` (same pattern already used there for `selfcare-cucumber-sdk`). Added
  `TenantValidationFilterTest` (`@QuarkusTest` + `quarkus-test-security-jwt`'s `@JwtSecurity`/`@Claim`)
  covering: matching header/claim (200), missing header (400), header/claim mismatch (400), hub-spid-login
  token missing the claim defaulting to `PNPG` with matching header (200) and with a different header
  (400), and an unknown tenant header value (400). Full `apps/product` suite (126 tests) passes with no
  regressions — pre-existing tests using `@TestSecurity` without JWT claims correctly bypass the filter
  (no issuer ⇒ no authenticated session ⇒ nothing to enforce), consistent with SELC-2.2 scoping.
  **Rollout completed for all remaining Quarkus consumers of `selfcare-sdk-security`**: `document-ms`,
  `iam`, `onboarding-ms`, `user-ms`, `webhook` all now have the same `quarkus.index-dependency` wiring
  and a `TenantValidationFilterTest` (6 cases each) against a real `@Authenticated` endpoint
  (`document-ms` → `GET /v1/documents/onboarding/{onboardingId}`, `iam` → `GET /iam/ping`,
  `onboarding-ms` → `GET /v1/onboarding`, `user-ms` → `GET /users/emails`). `webhook` has no
  `@Authenticated` endpoints at all (pure `X-Tenant-Id`-scoped webhook CRUD, no session auth), so its
  test (7 cases, including one confirming unauthenticated requests bypass the filter) targets
  `GET /info/version` instead — proving the filter still fires for any request carrying a valid JWT
  regardless of whether the hit endpoint itself requires `@Authenticated` (Priority
  `AUTHENTICATION + 100` runs for every request with a resolved `JsonWebToken`, not just protected
  ones; only requests with *no* JWT issuer at all are exempt, per SELC-2.2).
  Indexing `selfcare-sdk-security` in `iam` surfaced a genuine bean conflict: `iam` had its own
  `SecurityConfig` producing `it.pagopa.selfcare.security.JWTCallerPrincipalFactory` — the exact same
  lib class, manually re-wrapped in a producer method — which, once the lib jar was indexed, became
  ambiguous against the lib class's own `@ApplicationScoped @Alternative @Priority(1)` CDI bean
  (identical annotations, so CDI could not disambiguate). Removed `iam`'s redundant `SecurityConfig`
  (100% duplicate behavior, same `mp.jwt.verify.publickey` config property) rather than the lib bean;
  no functional change to `iam`'s JWT verification. All full suites pass with no regressions:
  `document-ms` 465, `iam` 61, `onboarding-ms` 482, `user-ms` 96 (pre-existing, unrelated classloader
  flake confirmed present on baseline before this change — not caused by this rollout), `webhook` 83.
  Sub-task 5 is now fully rolled out across every microservice depending on `selfcare-sdk-security` and
  every Spring Boot app with an inbound HTTP surface; still gated on sub-task 4 (`hub-spid-login` claim
  injection) for the claim side of the hub-spid-login flow to exist in production tokens.
  **Also wired into the Spring Boot services** (`dashboard-bff`, `external-api`, `user-group-ms`) since
  these are not on Quarkus and cannot depend on `libs/selfcare-sdk-security` (that jar carries Quarkus
  runtime dependencies unsuitable for a Spring Boot classpath). Each of the 3 apps gets a small,
  duplicated (not shared — see rationale below), self-contained `security.tenant` package: `TenantId`,
  `TenantConstants`, `TenantProblem` (plain POJOs) plus a `TenantValidationFilter` — a
  `@Component`-registered `OncePerRequestFilter` that naturally runs after Spring Security's
  `FilterChainProxy` (registered at Spring Boot's low default order), reads the issuer off the
  `SelfCareUser` principal already set by `selc-commons-web`'s `JwtAuthenticationFilter`/
  `JwtAuthenticationProvider` on `SecurityContextHolder`, and independently decodes the JWT payload
  (already cryptographically verified upstream — this filter only reads claims, it does not
  re-verify the signature) to read `tenant_id` via `Base64.getUrlDecoder()` + Jackson (both already on
  every Spring Boot classpath — zero new dependencies per `ARCHITECTURE.md` Dependency Rules). Same
  reconciliation/fail-closed/PNPG-default semantics as the Quarkus filter. Unit-tested per app (8 tests
  each, Mockito + a fake unsigned JWT payload) — no `@SpringBootTest`/real security chain needed since
  the filter only depends on `SecurityContextHolder` state and the request/response. Full suites pass
  with no regressions: `user-group-ms` 96 tests, `dashboard-bff` 379 tests, `external-api` 179 tests.
  A 4th Spring Boot app, `registry-proxy-runner`, was evaluated and **excluded**: it is a
  scheduler/batch service with no REST controllers or inbound HTTP surface, so there is no
  authenticated request to enforce tenant consistency on.
  **Duplication rationale:** no shared internal Spring library exists in this monorepo for these 4
  apps (they depend only on external `selc-commons-web`/`selc-starter-parent` artifacts published
  from another repo); creating a brand-new shared module was weighed against duplicating ~150 lines
  per app and the latter was chosen to avoid the release/versioning overhead of a new cross-app
  library for a 3-consumer, single-purpose filter.

### 6. Per-service tenant data isolation
- **Maps to:** SELC-7.2, SELC-7.3
- **Description:** For each microservice, choose and implement one of the two isolation models: discriminator
  field (tenant filter added to every Panache query) or database-per-tenant (Quarkus `TenantResolver` routing
  to per-tenant MongoDB connections, fail-closed on unresolved tenant). Inventory which services need which
  model.
- **Acceptance criteria:** No query path can return cross-tenant data; isolation model choice is documented per
  service.
- **Depends on:** Sub-task 5. **Blockers:** final model choice per service still `TO BE DECIDED` (SELC-7.3).

### 7. Deployment consolidation (parallel-run migration)
- **Maps to:** System purpose, SELC-5.3
- **Description:** Stand up the unified `infra/resources/<app>/{dev,uat,prod}` stacks alongside existing
  `-ar`/`-pnpg` stacks; migrate tenant-specific config/secrets to Key Vault references and prefixed env vars;
  cut traffic over at the APIM layer per environment; decommission legacy stacks, secrets, and certificates only
  after production validation.
- **Acceptance criteria:** Both tenants served by one stack per environment; legacy stacks fully decommissioned;
  no security regression during the overlap period (`SECURITY.md` Deployment/CI-CD rules).
- **Depends on:** Sub-tasks 2, 5, 6.

### 8. Scale and noisy-neighbor protection
- **Maps to:** ARCHITECTURE.md Scale expectations
- **Description:** Configure APIM per-tenant rate limiting/throttling and Container Apps min/max replica bounds
  sized from pre-migration APIM analytics, prioritizing concurrent-request-based autoscaling triggers (Quarkus
  reactive).
- **Acceptance criteria:** One tenant's traffic spike cannot degrade the other tenant's latency/availability;
  replica cap prevents MongoDB connection-pool exhaustion.
- **Depends on:** Sub-task 7. **Blockers:** exact thresholds `TO BE DECIDED` pending analytics.

### 9. Security hardening and audit logging
- **Maps to:** `SECURITY.md` (all sections)
- **Description:** Implement logging for tenant-mismatch rejections and the `hub-spid-login` default-to-`PNPG`
  path; enforce CORS restricted to both tenant frontend origins; verify Key Vault-only secret handling for any
  new tenant mapping/config; apply React CSP/XSS defaults on the frontend integration points.
- **Acceptance criteria:** Audit trail exists for tenant-validation failures and defaults; no secret or PII
  leakage in logs or error responses (`Problem`/RFC 7807 convention preserved).
- **Depends on:** Sub-tasks 2, 4, 5.

---

## Open blockers to resolve before/at epic kickoff
- Tenant claim name/format.
- `hub-spid-login` claim-injection mechanism.
- Final per-microservice data-isolation model (discriminator field vs. DB-per-tenant).
- Fine-grained authorization model (RBAC/ABAC/ReBAC), if needed beyond tenant scoping.
- Rate-limit thresholds and replica bounds (pending APIM analytics).
