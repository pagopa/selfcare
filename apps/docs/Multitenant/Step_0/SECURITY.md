# Security Rules — Multitenant Architecture

Source inputs: `apps/docs/Multitenant/Step_0/REQUIREMENTS.md`, `apps/docs/Multitenant/Step_0/ARCHITECTURE.md`.

## Required Security Inputs

Resolved since the previous revision (now reflected in Provisional Security Rules below): client framework
(React), APIM tenant-resolution signal (`Host` header, with client-supplied `X-Tenant-Id` explicitly stripped
and overwritten), the host→tenant mapping source of truth (Terraform-managed APIM Named Values / global
policy), the data-partitioning approach (hybrid: DB-per-tenant or discriminator field), the deployment
migration plan (parallel-run, decommission legacy after validation), and scale/noisy-neighbor protection
(APIM-level per-tenant throttling, capped Container Apps replicas).

Still open:
- Tenant claim name/format: `TO BE DECIDED` — JWT validation rules below are structural, not claim-specific, until resolved.
- `hub-spid-login` claim-injection mechanism: `TO BE DECIDED` (SELC-3) — until fixed, treat any injection point as a new trust boundary requiring its own review.
- Final per-microservice choice between DB-per-tenant vs. discriminator-field isolation (SELC-7.3 still lists both as options, not a per-service decision) — until settled, apply both rule sets below.
- Fine-grained authorization model (RBAC/ABAC/ReBAC) beyond tenant scoping: `TO BE DECIDED` — no model named in ARCHITECTURE.md.
- Exact rate-limit/throttling thresholds and min/max replica counts: `TO BE DECIDED` — ARCHITECTURE.md defers these to pre-migration APIM analytics.

## Provisional Security Rules

### HTTP / API boundary
- Reject requests with missing, unknown, or malformed `X-Tenant-Id` (SELC-1.3) — no silent default; return 4xx, not a generic 500.
- APIM MUST resolve tenant from the `Host` header and MUST unconditionally overwrite any client-supplied `X-Tenant-Id` before forwarding (ARCHITECTURE.md step 1) — treat an incoming client-set `X-Tenant-Id` as a spoofing attempt, never trust it past the edge.
- Enforce JWT-claim vs. `X-Tenant-Id`-header consistency on every request; hard-reject on mismatch (SELC-2.3), except the explicit `hub-spid-login` missing-claim → default-to-`PNPG` case (SELC-3.1), which still requires the header to be present and equal to `PNPG` (SELC-3.4).
- Validate all inbound JWTs (signature, `exp`, `iss`, `aud`) at both APIM and service level before trusting any claim — defense in depth, not APIM-only trust.
- Sanitize all user-controlled input with OWASP Java Encoder before use (existing repo convention) — applies equally to any new tenant-related fields.
- Restrict CORS to the known React frontend origins for both tenants (`selfcare.pagopa.it`, `imprese.notifichedigitali.it`) only; no wildcard origins (OWASP Cheat Sheet Series — CORS).
- Apply per-tenant + per-client rate limiting/throttling at the APIM layer to prevent one tenant from starving shared backend capacity ("noisy neighbor", ARCHITECTURE.md Scale expectations; OWASP API Security Top 10 — API4:2023); exact thresholds `TO BE DECIDED`.

### Authentication
- Both login flows (OneIdentity/`auth` and `hub-spid-login`) MUST converge on the same tenant-claim contract (SELC-6.1); do not let either flow define its own claim shape.
- Keep JWT signing/verification keys and per-tenant certificates in Key Vault, never in Terraform state or app config in plaintext (grounded in `infra/core/_modules/apim/jwt.tf`, `infra/core/_modules/jwt_keys`, `infra/core/_modules/key_vault`).
- When consolidating the two tenant-specific JWT-validation certs (`jwt-spid-crt`, `jwt-pnpg-spid-crt` in `infra/core/_modules/apim/jwt.tf`) behind one APIM, validate against the correct per-tenant key/cert, not a single shared one, unless/until a shared issuer is decided (Open Question in REQUIREMENTS.md).
- Any new component injecting claims into `hub-spid-login` tokens (SELC-3) must not weaken existing SPID assertion validation; treat it as a new trust boundary requiring its own review once designed.

### Authorization
- Treat `X-Tenant-Id` as authoritative only because APIM strictly overwrites it (see HTTP boundary rule above); backend services MUST still reconcile it against the validated JWT claim before authorizing (SELC-2.3) — never authorize on the header alone if a service is reachable other than via APIM.
- Do not implement tenant scoping via optional/best-effort checks; every authenticated endpoint must enforce tenant match before returning data (SELC-7.1).
- No RBAC/ABAC/ReBAC model is defined in ARCHITECTURE.md beyond tenant scoping — mark fine-grained authorization as `TO BE DECIDED`; do not invent a permissions model.

### Data tenancy isolation (SELC-7.3 hybrid model)
- **Discriminator field** (logical isolation): every Panache query/repository method MUST include the resolved tenant in its filter; a missing filter is a cross-tenant data leak, not a performance bug — treat any new repository method without a tenant filter as a security defect, not an oversight.
- **Database-per-tenant** (strong isolation): the Quarkus `TenantResolver` MUST fail closed (reject the request) if it cannot map the resolved tenant to a known database/connection — never fall back to a default database silently.
- Whichever model a given microservice adopts, the tenant used for isolation MUST be the one already validated per SELC-2.3/SELC-3.1, not re-derived independently at the data layer.

### Secret handling
- No secrets, tokens, or private keys in source, Terraform variables files, or logs (existing repo convention); reuse existing Key Vault-backed patterns (`infra/core/_modules/key_vault`) for any new multitenant configuration.
- APIM Named Values used for the host→tenant mapping (ARCHITECTURE.md step 1) MUST contain only non-secret routing data; any tenant-specific secret (e.g., `TENANT_PNPG_SECRET`, `TENANT_DEFAULT_SECRET`) MUST be a Key Vault reference, never a literal value in Terraform or in the Named Value/global policy.
- Pin any new credential/config needed for `hub-spid-login` claim injection to Key Vault-managed secrets, not environment variables checked into Terraform.

### Logging & error handling
- Log tenant-mismatch rejections (SELC-1.3/SELC-2.3) and any detected client-supplied `X-Tenant-Id` spoofing attempt with enough context to audit abuse, but never log full JWTs or raw tenant claims alongside PII.
- Keep the existing `Problem` (RFC 7807) error convention for tenant-validation failures; do not leak internal resolution logic (e.g., which cert/claim path failed, which DB/discriminator was used) in the response body — only in server-side logs.
- Log the missing-claim-default path (`hub-spid-login` → `PNPG`) explicitly, since it is a deliberate exception to strict validation and needs to be auditable.

### Deployment / CI/CD
- The parallel-run migration (new unified stack deployed alongside legacy `-ar`/`-pnpg`, cutover at APIM) MUST hold both the legacy and new stacks to the same security bar for the whole overlap period — do not relax cert/secret handling on either side "temporarily".
- Decommission legacy `-ar`/`-pnpg` Terraform state, secrets, and certificates only after the shared stack is validated in production (ARCHITECTURE.md Migration Strategy) — do not leave orphaned credentials active past cutover.
- Cap maximum Container Apps replicas per ARCHITECTURE.md Scale expectations to bound MongoDB connection-pool exhaustion — this is also a DoS control, not only a cost control.
- Any consolidation of `-ar`/`-pnpg` Terraform stacks (`infra/resources/<app>/{dev,uat,prod}-{ar,pnpg}` → `.../{dev,uat,prod}`, via `infra/resources/_modules/container_app_microservice`) must go through the same environment-gated pipeline (DEV auto, UAT auto, PROD manual approval) already in place — no new bypass path.
- Any new shared token-issuing layer or claim-injection service must be provisioned through the same Terraform/CI conventions as existing microservices, not as an out-of-band manual deployment.

### Client / frontend (React)
- Apply a restrictive Content-Security-Policy and standard React XSS defenses (avoid `dangerouslySetInnerHTML` with unsanitized data, escape all rendered user input) — OWASP Cheat Sheet Series: XSS Prevention, Content Security Policy (fallback — no React-specific prompt directory found).
- Frontend MUST NOT set or rely on a client-side `X-Tenant-Id`; tenant context for API calls must come only from the authenticated session/host, since APIM overwrites the header regardless.

## Selected Manicode Prompts

- `API security -> OWASP API Security Top 10 (fallback — no "Web and API Security" prompt directory found in repository)`
- `HTTP boundary (CORS, rate limiting, JWT validation) -> OWASP Cheat Sheet Series: CORS, JWT, Rate Limiting (fallback — no local prompt library)`
- `Backend framework -> Quarkus (no dedicated prompt directory found; apply Quarkus Security Guide + OWASP fallback)`
- `Client framework -> React: OWASP Cheat Sheet Series — XSS Prevention, Content Security Policy (fallback — no React-specific prompt directory found)`
- `Authentication, session model -> API Management (Azure) grounded in /infra/resources/webhook and /infra/core/_modules/apim/jwt.tf (fallback guidance: OWASP Cheat Sheet Series — JWT, Session Management)`
- `Authorization model -> /infra/* (no RBAC/ABAC/ReBAC prompt directory or library found; model itself is TO BE DECIDED per ARCHITECTURE.md — no default assumed)`
- `Data tenancy isolation (discriminator field / DB-per-tenant) -> no dedicated prompt directory found; fallback: OWASP Top 10 Proactive Controls (Access Control) + MongoDB/Quarkus multitenancy best practices`
- `Deployment / infra -> /infra/resources/_modules/container_app_microservice, /infra/core/_modules/container_app_environments (fallback guidance: OWASP Cheat Sheet Series — Docker/Container Security, CI/CD)`
- `Secret management -> /infra/core/_modules/key_vault, /infra/core/_modules/jwt_keys (fallback guidance: OWASP Secrets Management Cheat Sheet)`
- `Code quality -> OWASP Top 10 Proactive Controls (fallback — no "Code Quality" prompt directory found in repository)`
