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
- **Status: implemented.** Claim name and format are decided and in production code:
  `TenantConstants.TENANT_CLAIM = "tenant_id"`, values from the `TenantId` enum (`AR` for
  `selfcare.pagopa.it`, `PNPG` for `imprese.notifichedigitali.it`), both in
  `libs/selfcare-sdk-security` and mirrored in the Spring apps. The host→tenant mapping source of
  truth is the APIM `tenant_ids` variable (sub-task 2). Adding a tenant still requires a new enum
  constant, so SELC-6.2 ("extensible without code changes") is **only partly met** — a deliberate
  trade-off: a closed set is what lets every filter reject an unknown tenant fail-closed.

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
  (fail-closed). All 27 call sites (`dashboard-bff`, `onboarding-bff`, `iam`, `auth`, `webhook`,
  `registry-proxy`; every `dev`/`uat`/`prod` × `-ar` deployment folder that exists today, since the `-pnpg`
  folders are slated for deprecation once a single `-ar` deployment per environment serves both tenants) already
  declare **both** `AR` and `PNPG` in `tenant_ids`, so every API group is ready to accept and correctly label
  traffic from either frontend ahead of that consolidation. The `tenant_ids` origin table is centralized once in
  `infra/resources/_modules/local-env` (`local.tenant_frontend_origins`, keyed by `env`) and exposed as
  `module.local.config.tenant_ids` — every call site simply sets `tenant_ids = module.local.config.tenant_ids`,
  eliminating per-file duplication of the AR/PNPG origin list; the module orders its own tenant first
  (`local.domain`-aware), which now only decides which tenant local development origins map to.
  Backend consolidation itself (one Container App instead of `-ar`/`-pnpg` pairs) is deferred to sub-task 7.

  **Naming correction:** this sub-task's title and acceptance criteria say `Host` header, but the policy
  resolves on the caller's **origin** (`Origin`, falling back to a parsed `Referer`). That is the correct
  signal here and not a shortcut: within a single APIM instance every tenant frontend reaches the same
  gateway hostname, so `Host` does not distinguish them — the browser-declared origin does, and it is the
  same value CORS is already allow-listed against, so the two cannot drift apart. Read `Host` in the title as
  "the calling frontend URL".

  **Hardening applied after review.** The first implementation had three weaknesses, all now fixed and
  covered by a rendered-policy simulation of the resolution logic (legitimate AR/PNPG origins, suffix-attack
  domains, `Referer` with a path, case-shifted origins, origin-less requests, localhost in prod):

  1. *Prefix matching.* Resolution used `caller.StartsWith(declaredOrigin)`, so
     `https://selfcare.pagopa.it.attacker.example` matched the AR tenant and any attacker-controlled domain
     could obtain a valid `X-Tenant-Id` simply by prefixing a real tenant origin. It is now an **exact**
     dictionary lookup on the serialised origin; a `Referer` is first reduced to `scheme://authority` (so a
     path can no longer smuggle a prefix match), and both sides are lower-cased so casing cannot be used to
     evade the lookup.
  2. *Silent fallback.* Requests with neither `Origin` nor `Referer` silently resolved to `tenant_ids[0]`,
     which directly contradicts this sub-task's own acceptance criterion ("not defaulted") and was trivially
     reachable — any non-browser client could pick a tenant by simply omitting `Origin`. The fallback is now
     an explicit, per-API `default_tenant_id` variable defaulting to `null` (**reject with 403**). It is set
     on exactly two APIs, each with a stated reason: `auth`, because the SAML ACS (`loginSaml`) is a browser
     form-POST from the IdP and therefore legitimately carries the IdP's origin rather than a tenant
     frontend's — leaving it fail-closed would have broken login; and `webhook`, whose `external/webhook`
     base path is a server-to-server partner integration surface. Both resolve to `AR`, the only tenant that
     exposes those surfaces today. Every other API is now fail-closed.
  3. *`localhost:3000` in production.* It was hardcoded into the CORS allow-list and the tenant map of every
     API in every environment. Combined with `allow-credentials="true"` this let any process listening on
     port 3000 on a user's machine make credentialed calls to production. It is now the
     `local_development_origins` variable, sourced from `module.local.config.local_development_origins`,
     which is non-empty only when `env == "dev"`.
  4. *Subscription-bound callers could override their assigned tenant with `Origin`.* The first s2s mapping
     was consulted only when `Origin`/`Referer` was absent. Those headers are caller-controlled outside a
     browser, so an AR subscription could send the PNPG origin and receive `X-Tenant-Id: PNPG`. Subscription
     mapping is now evaluated first, regardless of those headers; a mapped credential cannot select another
     tenant.
  5. *The SAML fallback did not cover a real SAML POST.* `default_tenant_id` applied only when both browser
     headers were absent, while the IdP POST normally carries the IdP origin and was therefore rejected before
     the AR default. The fallback is now restricted by `default_tenant_operation_ids`; only `loginSaml` may
     resolve unknown-origin traffic to AR. Other auth operations remain fail-closed.
  6. *The external API minted claimless JWTs and trusted a caller header.* Every AR/PNPG JWT policy in
     `_modules/apim_external_api` now embeds the fixed tenant of that API topology in `tenant_id` and
     overwrites `X-Tenant-Id` with the same value. Existing subscription-key clients therefore do not need to
     supply a new header, and cannot claim the other tenant.

  Note this hardening is a **behavioural change**: callers that previously succeeded without an
  `Origin`/`Referer` header will now receive `403`. It must be observed in `dev` before promotion to `uat`
  and `prod`. The pre-existing per-operation policy on `auth`'s `loginSaml` still allow-lists
  `http://localhost:3000` in its own inline CORS block in all environments; that is outside this module and
  should be given the same treatment as a follow-up.

- **Follow-up: server-to-server callers routed through APIM had no way to express a tenant, and ran
  unscoped.** Found while sweeping sub-task 6's callers for `X-Tenant-Id` propagation. The design assumed
  every backend-to-backend call goes direct, callee private DNS name to callee private DNS name, carrying
  the caller's JWT and the `X-Tenant-Id` header — which is how `onboarding-ms`, `document-ms`,
  `institution-ms` and `auth`→`iam` call each other. Five clients across two apps do **not**:

  | Caller | Client | URL | Auth |
  |---|---|---|---|
  | `auth` | `internal.user-api` | `${INTERNAL_API_URL}` = `…/external/internal/v1` | subscription key |
  | `auth` | `internal.user-ms.api` | `${INTERNAL_MS_USER_API_URL}` = `…/internal/user` | subscription key |
  | `user-cdc` | `client.internal.delegation-api` | `${INTERNAL_API_URL}` | subscription key |
  | `user-cdc` | `client.internal.user-api` | `${INTERNAL_API_URL}` | subscription key |
  | `user-cdc` | `client.internal.user-group-api` | `${INTERNAL_API_URL}` | subscription key |

  These go **through APIM** and authenticate with `Ocp-Apim-Subscription-Key`, not a JWT. Two distinct
  problems, one of which is live today:

  1. *Present-tense data leak.* `TenantValidationFilter` returned early whenever there was no JWT issuer,
     so it never set `TenantContext`. `CurrentTenantProvider.currentTenantId()` then returned empty and
     every `tenantScoped(...)` call fell through to the unscoped query. So these five clients — including
     the **login path**, `auth` looking a user up in `user-ms` — read and wrote across **both tenants**.
     Fixed: the filter now scopes the request from a usable `X-Tenant-Id` when there is no JWT. It still
     never *rejects* unauthenticated requests (public and health endpoints must stay reachable), so the
     change is strictly additive. Trusting the header is safe only because APIM overrides it
     unconditionally on every request it forwards, and a service is not reachable from outside the
     private network without passing through APIM.

     The same early-return existed in **all seven** implementations of the filter — the Quarkus one in
     `libs/selfcare-sdk-security` plus the six independent Spring copies (`institution-ms`,
     `external-api`, `dashboard-bff`, `onboarding-bff`, `registry-proxy`, `user-group-ms`) — so all
     seven were fixed identically. That the same defect had to be fixed in seven places is itself an
     argument for extracting the Spring filter into a shared library, tracked as a follow-up.
  2. *Future 403.* Adding `X-Tenant-Id` in the callers' header factories would have been **useless** —
     APIM discards it. And once the tenant policy is applied to the APIs serving `external/internal/v1`
     and `internal/user`, these calls resolve to `default_tenant_id`, i.e. **403 fail-closed**: a live
     breakage of login, not a soft degradation.

  Widening `default_tenant_id` to cover it would reintroduce exactly the silent fallback point 2 above
  removed. Instead the module now takes **`service_caller_tenants`**, a map of APIM subscription id →
  tenant, evaluated before `default_tenant_id` and only for requests with no `Origin`/`Referer`. Each
  s2s caller's tenant is pinned to a credential it does not control, and one subscription is issued per
  *(calling service, tenant)* pair. `default_tenant_id` is now the last resort, for non-browser traffic
  that genuinely cannot be enumerated.

  Still open: the APIs serving `external/internal/v1` and `internal/user` are **not** declared through
  this module in this repo, so the mapping cannot yet be applied to them. Two options, to decide before
  `auth` or `user-cdc` are converted in sub-task 7 — (a) bring those APIs under `_modules/apim_api` and
  populate `service_caller_tenants`, or (b) re-point the five clients at the callees' private DNS names
  with a JWT, matching the pattern the rest of the estate already uses, which removes APIM from the path
  entirely. (b) is more consistent and is what `auth`→`iam` already does in the same codebase; (a) is
  smaller. Note `delegation-cdc` was checked and is **not** affected — its only outbound client is an
  `EventHubRestClient` — and `dashboard-bff`'s subscription-key interceptor targets the external PagoPA
  backoffice, not a tenant-scoped selfcare API.

  Because the filter lives in `libs/selfcare-sdk-security`, which every consumer pins at `0.2.3`, the fix
  reaches the services only once that library is republished. This follows the same in-place practice as
  the commit that first added the filter (`72de5545d`), which also did not bump the version.

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
- **Status: complete** — filter rolled out to all 6 Quarkus consumers of `selfcare-sdk-security` and
  all 6 Spring Boot services with an inbound HTTP surface, and the outbound propagation sweep is done.
  Originally wired into `product` as the proof-of-concept and verified end-to-end. Filter itself
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
  **Coverage gap closed (follow-up review):** an audit against the actual repository found that the
  "every Spring Boot app" claim above was wrong — three further Spring Boot services with a real
  inbound HTTP surface had been missed because they use a multi-module Maven layout
  (`app/`, `web/`, `core/`, `connector/`) rather than a top-level `src/`, so they did not show up in
  the original `apps/*/src/main` scan: `onboarding-bff` (10 `@RestController`s), `institution-ms` (8)
  and `registry-proxy` (18). All three authenticate via the same `selc-commons-web`
  `SelfCareUser` principal, and `onboarding-bff` is already declared in APIM's `tenant_ids`
  (sub-task 2) — so APIM was injecting `X-Tenant-Id` while the backend never validated it. The same
  `security.tenant` package has now been added to each, under the package each app's component scan
  actually covers (`it.pagopa.selfcare.onboarding.web.security.tenant`,
  `it.pagopa.selfcare.mscore.web.security.tenant`,
  `it.pagopa.selfcare.party.registry_proxy.web.security.tenant`), in the `web` module that already
  owns the controllers and the `selc-commons-web` dependency.
  **Issuer source differs in these three:** `onboarding-bff` pins `selc-commons 2.9.0`, whose
  `SelfCareUser` has no `getIssuer()` accessor (`2.9.1`+ does). Rather than bumping a shared
  dependency for an entire app, these three filters read the `iss` claim from the already
  cryptographically verified JWT payload they must decode anyway for `tenant_id`. This is
  version-independent and slightly stronger, since issuer and tenant claim are then guaranteed to
  come from the same token. Each has 9 unit tests (the 8 original scenarios plus one asserting that
  an authenticated token carrying no `iss` is not enforced rather than defaulted).
  Verification: `onboarding-bff` full suite 513 tests green; `registry-proxy` full suite 420 green;
  `institution-ms` targeted filter tests 9/9 green, but its **full suite cannot be validated
  locally** — `connector/rest` fails to compile its tests with a MapStruct
  `AooMapperImpl cannot be converted to AooMapper` error that **reproduces on a clean baseline with
  all these changes stashed** (baseline stops even earlier, at 378 tests, with the `web` module
  skipped). Pre-existing and unrelated to this work, but it must be fixed before `institution-ms`
  can be fully verified in CI.
  **Duplication rationale no longer holds — consolidation now recommended.** The original decision
  to duplicate rather than share was explicitly argued on "a 3-consumer, single-purpose filter".
  There are now **six** copies of a security-critical filter, in two slightly divergent variants
  (issuer from `SelfCareUser` vs. from the token payload). A change to the validation logic must now
  be applied six times and divergence is itself a security risk. Recommended follow-up: extract a
  `libs/selfcare-sdk-security-spring` module (plain Spring/servlet, no Quarkus dependencies),
  standardise on the token-derived issuer, and collapse all six copies onto it.
  **Duplication rationale:** no shared internal Spring library exists in this monorepo for these 4
  apps (they depend only on external `selc-commons-web`/`selc-starter-parent` artifacts published
  from another repo); creating a brand-new shared module was weighed against duplicating ~150 lines
  per app and the latter was chosen to avoid the release/versioning overhead of a new cross-app
  library for a 3-consumer, single-purpose filter.
  **Outbound propagation sweep — the enforcement filter had made every internal call fail.** Enforcing
  the header on inbound requests is only half of it: the filter rejects with **400** any request that
  carries a JWT but no `X-Tenant-Id`, and *no* caller in the monorepo was sending it. A full audit of
  every outbound code path (`ClientHeadersFactory` implementations on Quarkus, Feign
  `RequestInterceptor`/`@Import` configurations on Spring, and the multi-module `web`/`connector`
  layouts that the earlier scans had missed) produced this map, fixed in full:
  - **`onboarding-ms` → 5 clients** and **`onboarding-functions` → `document-ms`**: covered under
    sub-task 6 (machine token minted per-tenant for the functions app).
  - **`auth` → `iam`**: `IamMsHeadersFactory` sent the freshly minted session token with no header.
    `TokenContext` now carries the tenant that went into that token's `tenant_id` claim, and the
    header is derived from it — the header can never disagree with the claim it is validated against.
  - **`user-ms` → `onboarding-ms`**: now propagates the validated `TenantContext` (new
    `CurrentTenantProvider`, same pattern as `onboarding-ms`), never the raw inbound header.
  - **`dashboard-bff` and `external-api` → 15 internal Feign clients**: new `TenantHeaderInterceptor`
    reading the request attribute set by each app's `TenantValidationFilter`, imported per client
    alongside `AuthorizationHeaderInterceptor`. Deliberately **not** a `@Component`: a globally
    registered Feign interceptor attaches to every client, which would leak the internal tenant
    identifier to third parties (user-registry, PagoPA back-office). The `registry-proxy` clients are
    included — that service acquired the filter in this sub-task, so it is internal, not third-party.
  - **`institution-ms` → `user-ms`, `registry-proxy`** (3 Feign clients): same interceptor, placed in
    the `connector/rest` module. It matches the attribute as an `Enum` rather than importing
    `TenantId`, because that type lives in the `web` module and the module dependency runs the other
    way; an enum still rules out a `String` planted under the same attribute name.
  - **`institution-send-mail-scheduler` → `user-ms`** and **`onboarding-cdc` → `registry-proxy`**:
    these have no inbound request and authenticate with a pre-provisioned `JWT_BEARER_TOKEN`. The
    header is decoded **from that token's own `tenant_id` claim** (`MachineTokenTenantResolver`)
    rather than from a separate config value, which could drift out of sync with the token and turn
    every call into a mismatch rejection. If the provisioned token has no claim, no header is sent:
    the receiving service would reject for the missing claim anyway, and a fabricated header would
    only mask that the token must be re-issued. **Ops action required before release: re-issue both
    machine tokens with a `tenant_id` claim.**
  - **Deliberately untouched, verified not to need it:** `user-cdc` and two of `auth`'s three clients
    authenticate with `Ocp-Apim-Subscription-Key` and send no JWT, so the filter exempts them
    (SELC-2.2); `onboarding-bff`'s Feign clients carry no `Authorization` interceptor at all;
    `registry-proxy-runner` calls only Azure Search.
  - Tests: 5 new unit suites (18 cases) plus the existing ones, all suites green — `auth` 170,
    `institution-send-mail-scheduler` 13, `onboarding-cdc` 28, `dashboard-bff` 383,
    `external-api` 183, `institution-ms` 429. `user-ms`'s suite has a pre-existing `PartyRole`
    classloader linkage failure, confirmed identical on a stashed baseline; its new test passes when
    run on its own.

### 6. Per-service tenant data isolation
- **Maps to:** SELC-7.2, SELC-7.3
- **Description:** For each microservice, choose and implement one of the two isolation models: discriminator
  field (tenant filter added to every Panache query) or database-per-tenant (Quarkus `TenantResolver` routing
  to per-tenant MongoDB connections, fail-closed on unresolved tenant). Inventory which services need which
  model.
- **Acceptance criteria:** No query path can return cross-tenant data; isolation model choice is documented per
  service.
- **Depends on:** Sub-task 5.
- **Status: broad rollout implemented, but closure is blocked by the explicit migration items below.**
  - **Inventory (Cosmos DB / Mongo):** `auth`, `delegation-cdc`, `document-ms`, `iam`, `institution-send-mail-scheduler`,
    `onboarding-cdc`, `onboarding-functions`, `onboarding-ms`, `product`, `product-cdc`, `user-cdc`, `user-group-cdc`,
    `user-ms`, `webhook`, `user-group-ms` (Spring). **Chosen model: discriminator field** (`tenantId` on every
    document) for all of them — lower retrofit risk than database-per-tenant, and no regulatory/privacy constraint
    has been identified yet that would force stricter physical separation (`REQUIREMENTS.md` "Regulatory or privacy
    constraints" is still `TO BE DECIDED`; revisit database-per-tenant if that changes).
  - **Inventory (Azure Storage, source only):** `document-ms` (contracts/attachments — clearly tenant-scoped via
    onboarding, **in scope**), `dashboard-bff` (institution logos — shared/uncertain tenant-partitioning, **TO BE
    DECIDED**), `product` (contract templates — shared platform assets, **out of scope**, not per-tenant data),
    `registry-proxy-runner` (public IPA/ANAC registry cache — **out of scope**, not tenant data, same rationale as
    its sub-task-5 HTTP-surface exclusion). `product-cdc`/`user-cdc` use the shared
    `selfcare-onboarding-sdk-azure-storage` lib for CDC archival (different usage pattern; inherits whatever tenant
    marking exists upstream rather than an independent decision). **Chosen model where in scope: shared storage
    account, tenant discriminated at the container/path level** (extends `document-ms`'s existing per-purpose
    storage-account pattern) — no per-tenant storage account has been justified yet.
  - **`document-ms` proof-of-concept (implemented):**
    - Added `tenantId` field to the `Document` Mongo entity (nullable, additive).
    - Added tenant-scoped repository queries `findByOnboardingIdForTenant`/`findByIdForTenant` (additive, existing
      non-tenant-scoped queries left untouched) and tenant-scoped service overloads
      `getDocumentByOnboardingId(id, tenantId)` / `getDocumentById(id, tenantId)`: a document tagged for a
      different tenant is indistinguishable from a genuinely missing one (no cross-tenant existence leak).
    - **Migration-phase read filter:** the tenant predicate is `(tenantId = ?N or tenantId is null)`, not a strict
      equality. A strict filter would have made every pre-existing (untagged) document invisible to both tenants,
      turning both GET endpoints into a blanket 404 the moment the filter shipped — a self-inflicted outage that
      mock-based service tests could not detect. The `or tenantId is null` branch MUST be removed once the
      backfill has tagged every document; until then untagged documents remain readable by both tenants, which is
      the explicit, documented trade-off of the migration window.
    - **Tenant tagging on write:** `CurrentTenantProvider` (new, app-local) reads the validated `TenantContext`
      and degrades to an empty `Optional` instead of throwing when there is no active request scope
      (`ContextNotActiveException` — scheduled jobs, CDC consumers). `DocumentServiceImpl` tags documents at
      persist time (`buildDocument`, `persistDocumentForImport`) when a tenant is resolvable, and logs a warning
      + stores the document untagged otherwise, since service-to-service write calls (notably from
      `onboarding-ms`) do not propagate the tenant yet.
    - Wired `DocumentController`'s two `@Authenticated` GET endpoints (`/v1/documents/onboarding/{onboardingId}`,
      `/v1/documents/{id}` — the same ones covered by sub-task 5's `TenantValidationFilterTest`) to consume
      `TenantContext.getTenant()` (already populated by sub-task 5's filter) instead of re-deriving tenant, per
      SELC-8.5. Fails closed with `403` if, unexpectedly, no tenant was resolved.
    - Tests: 4 unit tests (tenant match / cross-tenant not-found) + 2 tagging tests (tenant applied on persist /
      left untagged when unresolvable) in `DocumentServiceImplTest`, and a new `DocumentRepositoryTenantTest`
      running the tenant-scoped queries against **real embedded MongoDB** (6 tests: same-tenant hit,
      cross-tenant miss, legacy-untagged still readable — for both query methods). The real-Mongo test is
      deliberate: the mock-based service tests pass regardless of whether the query matches any real data, so
      they are structurally incapable of catching the "filter matches nothing" class of bug. Full suite:
      **477/477 passing** (was 465 pre-sub-task-6; +12 new tests, 0 regressions).
  - **Remaining rollout (explicit follow-up, not done this iteration):** the other ~10 `DocumentRepository`
    methods (`updateContractFiles`, `updateAttachmentPathById`, `findAttachments`, `deleteDocument`, etc.) and the
    write/persist paths (`saveDocument`, `persistDocumentForImport`) are **not yet** tenant-scoped. Write paths are
    reached mostly via service-to-service REST calls from `onboarding-ms`, and it has not yet been verified whether
    those calls carry a forwardable tenant context — this needs investigation before tenantId can be safely set at
    persist time. The other 13 Mongo-using apps have not been touched at all; this proof-of-concept establishes the
    pattern (`TenantContext` injection + additive tenant-scoped query methods) to replicate per service.
  - **Blockers / required before this can be tightened:** (1) backfill script tagging every pre-existing
    document, after which the `or tenantId is null` branch must be dropped and the read filter becomes strictly
    fail-closed; (2) tenant propagation on service-to-service write calls from `onboarding-ms`, without which
    newly created documents keep landing untagged; (3) per-service rollout beyond `document-ms`. Until (1) and
    (2) land, `document-ms` tenant isolation is **partial and must not be relied upon as a security boundary** —
    it is additive defence, not enforcement.
  - **Per-product database isolation (product-driven routing) — implemented, `onboarding-ms` proof-of-concept.**
    A second, orthogonal axis was added on top of the tenant discriminator above: isolation is now also
    selectable **per product**, because the driver is commercial/contractual (a product may require its own
    database) rather than tenant-related. The two axes compose — a dedicated database still carries the
    `tenantId` discriminator, so a per-product database serving both tenants stays tenant-filtered.
    - **Configuration (owner: `product` microservice).** New `dataIsolation` block on the product entity:
      `database` (`SHARED` | `DEDICATED`) and `databaseName`. Absent block resolves to `SHARED`, so every
      existing product keeps today's behaviour with no migration. `ProductServiceImpl.validateDataIsolation()`
      rejects `DEDICATED` without a `databaseName` on both create and patch, so a product cannot be persisted
      in a state that would fail to route at runtime.
    - **Distribution.** The config had to be added in **all three** `Product` classes along the distribution
      chain (`apps/product` Mongo entity -> `apps/product-cdc` model + MapStruct `toResource()` -> JSON on Azure
      Blob -> `libs/selfcare-onboarding-sdk-product` entity read by consumers). `product-cdc` is the single hop
      where a dropped field would silently revert every consumer to `SHARED` without any error, so a pinning
      test in `ProductMapperTest` guards that propagation specifically.
    - **Contract hygiene.** Lombok's derived `isDedicatedDatabase()` helper leaked into both the OpenAPI schema
      and the blob-distributed JSON; it is now `@JsonIgnore` + `@Schema(hidden = true)`. Verified with a
      round-trip harness: helper absent from the JSON, `DEDICATED` survives write/read, a legacy document with
      no block resolves to `SHARED`.
    - **Routing (`onboarding-ms`).** `ProductDatabaseResolver` maps a productId to a database name, reading the
      product from the blob-backed SDK `ProductService`. It is **fail-closed**: an unknown product, a product
      lookup failure, or a `DEDICATED` product with no `databaseName` raises
      `UnresolvableProductDatabaseException` rather than falling back to the shared database — falling back
      would write a product's data into the very database it was configured out of.
      `OnboardingMongoDatabaseResolver` implements Quarkus' `MongoDatabaseResolver` and is what Panache calls
      on every entity operation.
    - **Why a request-scoped holder.** Quarkus' `MongoDatabaseResolver#resolve()` takes **no arguments**, so the
      product cannot be passed to it down the call chain; `ProductRoutingContext` (`@RequestScoped`) is that
      channel, set in `OnboardingPersistenceHelper` where the product is known. Outside an active request
      (orchestration callbacks, scheduled jobs) there is no scope to read and the shared database is used —
      correct rather than a fallback, since such callers are not serving a product-scoped request.
    - **Discriminators on write.** `Onboarding` and `Token` now carry `tenantId` alongside the existing
      `productId`. `OnboardingPersistenceHelper.stampTenant()` tags at the single persist chokepoint, never
      reassigns an onboarding that already has a tenant (so replays/updates cannot move a record between
      tenants), and leaves it untagged when no tenant is resolvable rather than guessing.
    - **Tests: 499/499 passing** (was 493; +17 new, 0 regressions). Includes a `@QuarkusTest` **wiring** test:
      Panache discovers the resolver via `Arc.container().select(MongoDatabaseResolver.class)`, so the routing
      would silently stop working if the bean stopped being resolvable while every unit test still passed —
      the wiring test asserts the container really hands Panache our implementation.
    - **Not done / open.** (a) The ~12 endpoints identified only by `onboardingId` (`/{onboardingId}`,
      `/reject`, `/approve`, `/complete`, ...) still cannot resolve a dedicated database, because the product is
      unknown before the document is read and the no-arg resolver cannot be told which database to open — they
      work today only because every product is `SHARED`. This **must** be solved before any product is switched
      to `DEDICATED`. (b) Read paths are not yet tenant-filtered (only writes are stamped). (c) Cosmos access is
      still via connection string from Key Vault, not managed identity, so the "same identity, second database"
      premise is not yet true in infrastructure. (d) No Terraform module exists yet for a dedicated Cosmos
      database.
  - **`document-ms` tenant enforcement + service-to-service propagation — implemented.**
    - **Latent break found first.** `TenantValidationFilter` is a global `@Provider` and `DocumentController` is
      class-level `@Authenticated`, so *every* endpoint is filtered — including the service-to-service ones
      (`/contract-files`, `/import`, `/save`). The filter answers **400** to any request that carries a JWT but
      no `X-Tenant-Id`, and neither caller (`onboarding-ms`, `onboarding-functions`) sent that header: they
      forwarded `Authorization` only. Reproduced with the existing
      `getDocumentByOnboardingId_withMissingHeader_shouldReturnBadRequest` test rather than by inspection. Not a
      live outage (branch unmerged) but it would have broken onboarding document upload on release.
    - **Propagation (`onboarding-ms`).** `AuthenticationPropagationHeadersFactory` — the single chokepoint for
      all 5 downstream REST clients — now injects `CurrentTenantProvider` and adds `X-Tenant-Id`. It propagates
      the **validated** `TenantContext` value, never the raw inbound header: the context value has already been
      reconciled against the JWT claim, so echoing the header would forward unvalidated input if the filter were
      ever bypassed for some path.
    - **Propagation (`onboarding-functions`) — machine tokens.** The functions app mints its own JWT, which
      carried no tenant claim; a header alone would still be rejected downstream. Per decision, the machine
      token is now **minted per-tenant**: `Onboarding` carries `tenantId`, `Utils.readOnboardingValue` (the
      single deserialisation chokepoint) publishes it into a `FunctionTenantContext` ThreadLocal — the app has
      no CDI request scope at all — and `createJwt(userId, tenantId)` embeds the claim. The header is sent
      **only** when the minted token corroborates it; when the pre-provisioned env token (`JWT_BEARER_TOKEN`) is
      used no claim can be added, so the header is deliberately omitted rather than sent alone, which would
      produce exactly the header/claim mismatch the downstream filter rejects. Staleness of the ThreadLocal is
      mitigated by `readOnboardingValue` always overwriting, including with `null`.
    - **Implicit tenant filtering in the repository.** The earlier `...ForTenant` duplicated-method pattern was
      removed: with both variants callable, any caller could silently opt out and every newly added method
      started unscoped. `DocumentRepository` now injects `CurrentTenantProvider` and applies a private
      `tenantScoped()` filter inside **all ~12** query/update/delete methods, so no query can reach MongoDB
      unscoped. The two-argument service overloads were dropped accordingly.
    - **Tests: 487/487 passing** in `document-ms`, plus 499/499 (`onboarding-ms`) and 367
      (`onboarding-functions`) with no regressions. `DocumentRepositoryTenantTest` was rewritten against **real
      embedded MongoDB** (20 tests) — mock-based repository tests cannot catch a filter that compiles but
      matches nothing.
    - **Still open.** The `or tenantId is null` branch remains until the backfill lands, so enforcement stays
      additive defence rather than a hard boundary. **The same service-to-service break very likely affects
      `user-ms`, `iam`, `product` and `webhook`**, whose callers (`institution-send-mail-scheduler`,
      `onboarding-cdc`, `user-cdc`, `auth`) were inspected and none send `X-Tenant-Id`; a systematic sweep is
      required before release.
  - **Full rollout across the remaining services — implemented.** The `document-ms` pattern (tenant filter applied
    *inside* the data-access layer, never as an opt-in `...ForTenant` variant) was replicated across every
    remaining Mongo-using app. Isolation model everywhere: **discriminator field** with the migration predicate
    `tenantId == current OR tenantId == null`; writes are stamped at the narrowest chokepoint and **only when
    `tenantId` is unset**, so a replay or an update can never move a record between tenants.

    | App | Chokepoint | Scoped collections | Tests |
    |-----|-----------|--------------------|-------|
    | `iam` | `UserClaims` entity + `UserPermissionsRepository` aggregations | `userClaims`, `userPermissions` | 61 → 67 |
    | `auth` | new `OtpFlowRepository` | `otpFlow` | 129 → 133 |
    | `user-group-ms` | `UserGroupServiceImpl` (the one layer both `MongoRepository` writes and `MongoTemplate` reads pass through) | `userGroups` | 96 → 104 |
    | `user-ms` | `QueryUtils` (~20 call sites) + 5 bypasses in `UserInstitutionServiceDefault`/`UserInfoServiceDefault` | `userInstitutions`, `userInfo` | 6 new tests |
    | `institution-ms` (Spring) | `MongoCustomConnectorImpl` + repository defaults, via new `TenantDataIsolation` | `Institution`, `Delegations`, `MailNotification` | 429 → 433 |
    | `onboarding-ms` | `QueryUtils.buildQuery(...)` (now a CDI bean) + id/string/update bypasses | `onboardings`, `tokens` | 499 → 504 |
    | `delegation-cdc` | `DelegationRepository` / `InstitutionRepository` | mirror collections | 19 → 23 |
    | `user-cdc` | `UserInstitutionRepository` | `userInfo`, aggregate `userInstitutions` | 47 → 50 |
    | `user-group-cdc` | entity + notification mapper | mirror collection | 1 → 2 |
    | `onboarding-cdc` | `Onboarding` entity + mapper | mirror collection | 28 |
    | `institution-send-mail-scheduler` | `MailNotification` entity + mail parameters | `MailNotification` | 13 |

    - **CDC apps and the scheduler get *propagation*, not request-scoped filtering.** They have no request context
      and no `TenantContext`, so there is nothing to filter *by*: they read a change stream that spans both
      tenants. Their job is to carry the `tenantId` already present on the source document through to the mirror
      collection and the outbound event, so downstream consumers can route without re-reading the source. The
      cross-tenant batch query in `InstitutionSendMailScheduledServiceImpl.runQueryAndSendNotification` is
      therefore **deliberately unscoped**, with an inline comment saying so — it is not a security boundary.
    - **Event payloads.** `delegation-cdc`'s payload class is app-local, so `tenantId` is a plain field.
      `user-cdc` and `user-group-cdc` publish payload types owned by `selfcare-user-sdk-model`; since that
      library is pinned per-app and resolved from a remote repository, the field is added via app-local
      `TenantAware*` subclasses rather than by bumping and republishing the shared contract. Jackson serialises
      the runtime type, so the claim reaches the wire either way.
    - **`auth` cannot use `TenantContext`** — it issues sessions *before* one exists — so it resolves the tenant
      from the request via `TenantHeaderUtils.resolveTenantId(...)`, which is **fail-closed**.
    - **Real security hole found and closed in `auth`.** OTP verification looked the flow up by UUID with no
      tenant predicate, so an OTP issued through the PNPG frontend could be redeemed from the AR frontend (and
      vice versa). The new `OtpFlowRepository` scopes every OTP lookup by tenant; the tenant is passed
      explicitly because there is no `TenantContext` to read.
    - **Cross-tenant writes fail loudly in `institution-ms`.** `TenantDataIsolation.stampTenantForSave` first
      silently returned the pre-existing entity when the current tenant did not own it — the caller's write was
      discarded while the call still looked successful. It now raises `InvalidRequestException`: a rejected
      cross-tenant write must be observable, not silently swallowed.
    - **`CurrentTenantProvider` is duplicated per app rather than added to `libs/selfcare-sdk-security`.** The
      library is pinned per-app at `0.2.3` and CI resolves it from a remote repository, so a shared helper would
      require a version bump plus republish across every consumer — out of scope for this branch. The
      duplication is intentional and should be collapsed when the library is next versioned.
    - **Explicitly excluded.** `product` and `product-cdc` hold the **global product catalogue** — including the
      `dataIsolation` block that *drives* the routing decision above — so it is shared by all tenants by
      definition; scoping it would make products invisible per tenant. `iam`'s `Roles` collection is excluded for
      the same reason (global role catalogue). `webhook` was inspected and is already tenant-aware.
      `registry-proxy` caches the public IPA/ANAC registry, which is not tenant data.
    - **Verification.** Every suite was re-run and compared against a clean `git worktree` of the branch head
      before accepting the result — an agent-reported "pre-existing failure" in `user-cdc` turned out to be a
      stale `libs/*/target` in the shared working tree (Quarkus resolves workspace modules from `target/classes`
      in preference to the `~/.m2` jar, producing `LinkageError`s that look like code regressions). Rebuilding
      the libraries restored 47/47 at baseline and 50/50 with the change.
    - **Still open (unchanged from `document-ms`).** The `tenantId == null` branch stays until the backfill has
      tagged every document, so isolation remains additive defence rather than a hard boundary; and the
      service-to-service `X-Tenant-Id` sweep flagged above is still required for `user-ms`, `iam`, `product` and
      `webhook`.
  - **Review fixes after the rollout:**
    - `document-ms` had two attachment creation paths in `DocumentContentServiceImpl` that persisted directly
      and bypassed the tenant stamping used by `DocumentServiceImpl`; both now stamp from the validated
      `CurrentTenantProvider`.
    - `delegation-cdc`'s tenant-aware institution lookup queried `id`, but Mongo stores the Panache identifier
      as `_id`; tenant-bearing events now resolve the institution correctly.
    - `onboarding-functions` now carries `tenantId` through `OnboardingAggregateOrchestratorInput`, publishes
      it into `FunctionTenantContext` when deserialising aggregate activities, and scopes the bulk
      reject/override updates by `(tenantId = current OR tenantId = null)`. The previous direct `updateMany`
      paths could mutate another tenant's onboardings sharing institution/product identifiers.
    - The functions machine-token fallback now reads `tenant_id` from the exact token it sends and refuses a
      tenant-scoped call if the claim is absent or disagrees with an activity tenant. Legacy activities whose
      payload has no tenant derive the header from that same deployment-scoped token, which is safe while AR
      and PNPG functions remain separate; those payloads must gain `tenantId` before consolidation. Re-issuing
      the per-deployment token with the appropriate claim remains an operational prerequisite.
    - `institution-ms` still has global `externalId` uniqueness, which prevents the same institution from
      existing independently under both tenants. This cannot be changed in-place: Cosmos DB for MongoDB only
      permits creating a unique index while the collection is empty. The required fix is a collection
      migration/cutover that creates `(tenantId, externalId)` uniqueness on the empty target before loading
      the backfilled data; changing the Terraform index on populated production collections would only make
      deployment fail.
  - **New blocker found by review — `user-cdc` mirror identity.** `userInfo._id` is still the bare PDV
    `userId`, while reads now treat `(tenantId, userId)` as distinct. A person present in both tenants makes the
    second insert fail with duplicate `_id`. Fixing this safely requires a coordinated schema migration in
    both `user-cdc` and `user-ms` (tenant-qualified/surrogate `_id`, retained logical `userId`, all ID-based
    mutations updated, and existing documents migrated). It is not safe to change one producer class in
    isolation.
  - **New blocker found by review — cross-tenant scheduler credentials.**
    `institution-send-mail-scheduler` intentionally reads both tenants' notifications but has one
    `JWT_BEARER_TOKEN`; after deployment consolidation it cannot call `user-ms` for records owned by the other
    tenant. The consolidated stack needs tenant-specific machine credentials and record-scoped selection
    before this worker can process both tenants.

### 7. Deployment consolidation (parallel-run migration)
- **Maps to:** System purpose, SELC-5.3
- **Description:** Stand up the unified `infra/resources/<app>/{dev,uat,prod}` stacks alongside existing
  `-ar`/`-pnpg` stacks; migrate tenant-specific config/secrets to Key Vault references and prefixed env vars;
  cut traffic over at the APIM layer per environment; decommission legacy stacks, secrets, and certificates only
  after production validation.
- **Acceptance criteria:** Both tenants served by one stack per environment; legacy stacks fully decommissioned;
  no security regression during the overlap period (`SECURITY.md` Deployment/CI-CD rules).
- **Depends on:** Sub-tasks 2, 5, 6.
- **Status: pattern established; `iam`/dev and `product`/dev conversions written and `terraform validate`-clean;
  nothing applied, imported or wired into CI.**
  - **Inventory (`infra/resources/`, 24 app stacks).**
    - **13 apps are deployed twice** (`{dev,uat,prod}-ar` *and* `-pnpg`) and are the actual consolidation
      scope: `dashboard-bff`, `external-api`, `iam`, `institution-ms`, `onboarding-bff`, `onboarding-cdc`,
      `onboarding-functions`, `onboarding-ms`, `product`, `registry-proxy`, `user-cdc`, `user-group-ms`,
      `user-ms`. 39 stacks collapse to 13.
    - **10 apps are `-ar` only** (`api`, `auth`, `delegation-cdc`, `document-ms`,
      `institution-send-mail-scheduler`, `namirial-sign`, `product-cdc`, `registry-proxy-runner`,
      `user-group-cdc`, `webhook`). These are **not** a consolidation task — there is nothing to merge — but
      they are the opposite risk: they already run as a single deployment and will start receiving PNPG
      traffic, so their sub-task 5/6 tenant handling has to be correct *before* cutover, not after. `auth` is
      the exception that stays AR-specific by design: PNPG authenticates through `hub-spid-login`.
    - **1 app is `-pnpg` only** (`spid-login`, i.e. `hub-spid-login`). It is tenant-specific by nature and is
      out of scope for consolidation.
  - **Two classes of conversion, and they are not equally hard.**
    - **Stateless (7 apps: `dashboard-bff`, `external-api`, `onboarding-bff`, `onboarding-cdc`,
      `onboarding-functions`, `registry-proxy`, `user-cdc`)** — no Cosmos database of their own, so
      consolidation is config-only and reversible.
    - **Stateful (6 apps: `iam`, `institution-ms`, `onboarding-ms`, `product`, `user-group-ms`, `user-ms`)** —
      each has a Cosmos database in *both* the AR and the PNPG account. Consolidating the deployment means
      merging two databases, which is a one-way data migration, not a Terraform change. This is the real cost
      of sub-task 7 and it is gated on sub-task 6's backfill.
  - **Reference conversion: `infra/resources/iam/dev/` (new).** `iam` was picked because its two legacy stacks
    differ only in naming, DNS and one JVM flag, so the pattern could be established without also resolving
    app-specific divergence. The stack:
    - Deploys **one** container app (the AR one survives) against **one** Cosmos database and **one** Key Vault.
    - Keeps **both** APIM APIs — `iam` on the AR hostname and `imprese/iam` on the PNPG hostname — both
      pointing at that single backend. Neither frontend changes, and rollback is re-pointing one
      `service_url` rather than rebuilding infrastructure. Collapsing the two URLs into one is a separate,
      frontend-visible decision and is deliberately excluded.
    - Relies on the fact that tenant identity does **not** depend on which API is called: `_modules/apim_api`
      resolves `X-Tenant-Id` from the calling origin against the `tenant_ids` registry and discards any value
      the caller sent, so a PNPG browser reaching the AR API is still resolved as `PNPG` and the two APIs
      cannot be played off against each other.
    - Changes the `userClaims` unique index from `email` to composite `(tenantId, email)`. The same person can
      hold claims under both tenants, so a globally unique `email` makes the database merge fail on duplicate
      keys for every user present in both.
  - **"Alongside" is not achievable as written, and the plan was changed rather than faked.** A genuinely
    parallel stack would have to stand up a second container app plus a third and fourth APIM API — more
    moving parts to keep in sync than the migration it is meant to de-risk. The unified stack is instead a
    **successor** that adopts the existing AR resources and the existing PNPG APIM API into a new state file
    via `terraform import`. Nothing is destroyed and recreated. The consequence is documented prominently:
    applying this stack before importing would try to create resources that already exist.
  - **State ownership correction after review.** `terraform import` alone is forbidden for these conversions:
    it copies an existing resource into the unified state but leaves the legacy state able to update or destroy
    it. Both runbooks now require pipelines to be disabled, all state files backed up, and every module child
    resource (including locks, DNS records and alerts) moved out of the legacy state and into the unified state.
    Cutover cannot proceed until no Azure resource id is owned by more than one state.
  - **Not wired into CI on purpose.** `pr_iam_infra.yml` does not plan the new folder. Its state file does not
    exist yet, so every pull request would show "create 12 resources" for resources that must be imported, not
    created — a permanently red plan that invites approving the wrong thing. The plan job belongs in the same
    change that performs the import.
  - **Hard prerequisites identified (each fails silently, not loudly, if skipped)** — full detail in
    `infra/resources/iam/dev/README.md`:
    1. Sub-task 6's backfill must have run **and** the `or tenantId is null` migration branch must be removed
       *before* the merge. Merging two databases of untagged documents produces one database in which the
       current permissive filter shows every legacy PNPG document to AR users and vice versa.
    2. The composite unique index must be applied *before* the data import, not after.
    3. `JWT_PUBLIC_KEY` must verify **both** issuers (`auth` for AR, `hub-spid-login` for PNPG). Each legacy
       stack held only its own key; if the issuers do not share one, every PNPG request fails authentication
       the moment traffic moves, and sub-task 4 has to land first.
    4. Any secret that genuinely differs per tenant must become a per-tenant env var resolved app-side, never
       a single collapsed value.
  - **Remaining: 12 of 13 apps × 3 environments (36 stacks), plus `iam` uat/prod.** Deliberately not attempted
    in one pass — each stateful app carries its own data migration, and a bulk conversion would produce a large
    unreviewable diff whose riskiest parts (the data merges) are invisible in it.
  - **The stateless/stateful split turned out to be the wrong ordering criterion.** Two findings from converting
    the second app contradict the "stateless = config-only and reversible" framing above, which is left in place
    because it is what the inventory looked like before the stacks were read line by line:
    1. **The two stacks are in different Container App Environments** (`selc-d-cae-002` /
       `selc-d-container-app-002-rg` for AR versus `selc-d-pnpg-cae-cp` / `selc-d-container-app-rg` for PNPG),
       with different private DNS domains. Consolidating any app means its PNPG callers must reach the AR
       environment. That is a network prerequisite for *every* conversion, stateless included, and it fails as a
       connection timeout in the caller rather than as an error in the converted stack.
    2. **Callers are wired by private DNS name, so conversions are ordered by the call graph, not by statefulness.**
       An app cannot be consolidated before the services it calls, because its `MS_*_URL` values point at
       `-pnpg-`-prefixed container apps that must still exist. Derived from the `dev-ar` stacks:

       | Layer | Apps | Calls |
       |-------|------|-------|
       | Leaves | `product`, `user-group-ms`, `user-cdc`, `product-cdc`, `delegation-cdc`, `user-group-cdc`, `webhook` | nothing internal |
       | Tier 1 | `institution-ms`, `onboarding-ms`, `registry-proxy`, `user-ms`, `iam`, `onboarding-cdc`, `institution-send-mail-scheduler`, `document-ms` | leaves + each other |
       | Tier 2 (entry points) | `dashboard-bff`, `onboarding-bff`, `external-api`, `api`, `onboarding-functions` | tier 1 |

       The BFFs and gateways must therefore be converted **last**, not first — the opposite of what "stateless
       first" would suggest. Note `iam`, the reference conversion, calls `institution-ms` and so is not itself
       deployable first.
  - **Config divergence measured per app** (distinct env-var *names* present in only one of the two `dev` stacks):
    `external-api`, `iam`, `onboarding-functions`, `product`, `user-group-ms` = **0** (pure naming/DNS
    conversions); `onboarding-bff` and `onboarding-cdc` = 1; `dashboard-bff` = 4; `institution-ms` = 4;
    `onboarding-ms` = 6; `user-ms` = 8; `user-cdc` = 12; `registry-proxy` = 21. A non-zero count is
    **application** work, not Terraform work: the flag has to become tenant-aware config or be reconciled to a
    single value. `user-cdc` is the clearest case — `USER_CDC_SEND_EVENTS_WATCH_ENABLED` and
    `USER_CDC_SEND_EVENTS_FD_WATCH_ENABLED` are set on AR only, so today PNPG emits no user events at all.
    Consolidating it without a decision would silently start emitting PNPG events to AR consumers.
  - **Second conversion: `infra/resources/product/dev/` (new), `terraform validate`-clean.** `product` is the
    only app in scope that is simultaneously a call-graph **leaf**, **zero-divergence**, and **not exposed
    through APIM** (internal-only, so no `service_url` and no frontend-visible change) — the smallest possible
    end-to-end exercise of the runbook.
    - The PNPG-only JVM DNS flags (`-Djava.net.preferIPv4Stack=true`, `networkaddress.cache.*`) are dropped
      rather than parameterised: they were workarounds for the PNPG container app environment, which the unified
      stack does not use.
    - **The hard part is not Terraform — it is that two catalogues become one.** Because `product` is
      deliberately excluded from tenant discrimination (sub-task 6: the catalogue is global platform
      configuration), the merge *cannot* be done by tagging and copying documents. The two catalogues have
      drifted independently, so every `productId` present in both with **different** configuration has no
      automatic resolution: picking one silently changes behaviour for the tenant whose version lost, and that
      change is invisible in the Terraform diff. `contractTemplates` has a unique index on
      `(productId, name, version)`, so unresolved duplicates fail the import partway through and leave a
      half-merged catalogue. `infra/resources/product/dev/README.md` requires the reconciliation to be produced
      and signed off *before* the cutover window, not during it.
    - `product-cdc` publishes the catalogue to blob storage for every consumer of
      `libs/selfcare-onboarding-sdk-product`. It is `-ar`-only so it needs no consolidation, but it must be
      re-pointed at the merged database in the same change or consumers keep serving the pre-merge snapshot.

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
- ~~Tenant claim name/format.~~ **Resolved:** `tenant_id`, values `AR`/`PNPG` (sub-task 1).
- `hub-spid-login` claim-injection mechanism. **Still the one hard blocker**: until it lands, PNPG
  session tokens carry no claim and rely on the filter's default-to-`PNPG` fallback, which cannot
  distinguish a genuine PNPG session from a claim that was dropped.
- ~~Final per-microservice data-isolation model (discriminator field vs. DB-per-tenant).~~
  **Resolved:** both, on orthogonal axes — a `tenantId` discriminator everywhere, plus per-product
  database routing driven by the product's `dataIsolation` config (sub-task 6).
- Fine-grained authorization model (RBAC/ABAC/ReBAC), if needed beyond tenant scoping.
- Rate-limit thresholds and replica bounds (pending APIM analytics).
- `userInfo` tenant-qualified identity migration coordinated between `user-cdc` and `user-ms`.
- Tenant-specific machine credentials for the consolidated cross-tenant mail scheduler.
- `institution-ms` collection migration to replace global `externalId` uniqueness with
  `(tenantId, externalId)` on an empty target collection before data load.
- Re-issue the per-deployment `onboarding-functions` machine token with its `tenant_id` claim before enabling
  tenant-enforcing downstream calls.
- Add `tenantId` to the remaining tenantless `onboarding-functions` activity payloads before consolidating
  AR and PNPG functions; deployment-token fallback is valid only while deployments remain tenant-specific.
