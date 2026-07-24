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

### 3. `auth` microservice: tenant claim issuance (OneIdentity flow)
- **Maps to:** SELC-4
- **Description:** Extend `auth` to add the tenant claim to JWTs it issues, using the same resolution strategy
  as APIM (sub-task 1).
- **Acceptance criteria:** JWTs issued via OneIdentity login contain the tenant claim, consistent with the
  `X-Tenant-Id` the same client would receive from APIM.
- **Depends on:** Sub-task 1.

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
