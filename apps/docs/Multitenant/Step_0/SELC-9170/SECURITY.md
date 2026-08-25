# SELC-9170 - Remaining Multitenant Authentication Security

Source inputs:
`apps/docs/Multitenant/Step_0/SELC-9170/REQUIREMENTS.md` and
`apps/docs/Multitenant/Step_0/SELC-9170/ARCHITECTURE.md`.

No local Manicode `PROMPT.md` library is present in this repository. The rules
below therefore use the concrete repository infrastructure and the closest
public OWASP guidance as explicitly identified in
`Selected Manicode Prompts`.

## Required Security Inputs

Known:

- APIM derives the tenant from the request host and overwrites
  `X-Tenant-Id`.
- Canonical tenant values are `AR` and `PNPG`.
- `auth` issues JWTs containing `tenant_id`.
- Tenant signing keys are supplied to the Azure Container App through Key Vault
  references and managed identity.
- `auth` validates all enabled tenant signing keys during startup and does not
  become ready if any required key is missing or invalid.
- `infra/cert` manages signing certificates and private material in Key Vault,
  then publishes each tenant's verification certificates with `kid` values in
  the `.well-known/jwks.json` hosted by that tenant's CDN storage.
- Development tenant certificate lifecycle will be consolidated under
  `infra/cert/dev-ar`, using `_ar` and `_pnpg` certificate-name suffixes.
- Verification keys are selected using the APIM-resolved tenant and JWT `kid`;
  `tenant_id` is checked only after signature verification.
- `dashboard-bff` JWT exchange tokens will contain the validated `tenant_id`.
- A verified `hub-spid-login` token may temporarily omit `tenant_id`; only that
  case may resolve to `PNPG`.
- The cryptographically verified issuer identifies the `hub-spid-login`
  compatibility path.
- Header/claim mismatch and invalid PNPG fallback requests return
  `400 Bad Request` using the RFC 7807 `Problem` convention.
- Header/claim reconciliation and service-to-service tenant propagation are
  already implemented across the Step 0 backend consumers.
- Both tenant frontends use React.
- The current rollout keeps one AR `auth` deployment per environment tier;
  PNPG remains on `hub-spid-login`.
- The server is Java 17 with Quarkus, RESTEasy Reactive, SmallRye JWT, and
  MicroProfile REST Client.

Still open:

- Rotation overlap duration and revocation approval procedure:
  `TO BE DECIDED`.
- Regulatory and privacy constraints: `TO BE DECIDED`.
- Exact production rate limits and Container App replica bounds:
  `TO BE DECIDED` from APIM analytics.

## Provisional Security Rules

### HTTP and APIM trust boundary

- APIM MUST derive tenant identity only from the configured host allowlist and
  MUST overwrite, not preserve, any client-supplied `X-Tenant-Id`.
- The backend MUST reject missing, blank, malformed, unknown, or disabled tenant
  values. Tenant identifiers MUST match canonical configured values exactly.
- Direct access to the Container App MUST be restricted to APIM or protected by
  an equivalent authenticated gateway-to-backend control. If callers can reach
  the backend directly, `X-Tenant-Id` alone MUST NOT be treated as trusted.
- CORS MUST use explicit approved origins and headers. Wildcard origins MUST NOT
  be combined with credentials.
- APIM SHOULD rate-limit login and token-issuing operations by tenant and client.
  Thresholds remain `TO BE DECIDED`.

Sources: `infra/resources/_modules/apim_api`,
`infra/resources/_modules/container_app_microservice`; fallback to OWASP REST
Security, CORS, and Denial of Service Cheat Sheets and OWASP API4:2023.

### Tenant-specific JWT issuance

- `auth` MUST select signing material from the trusted server-side tenant
  context established for the request, never from request bodies, query
  parameters, or unverified JWT claims.
- Each enabled tenant MUST resolve only to its configured signing key. Missing,
  malformed, or inaccessible key material MUST fail application startup; no
  global, default, or cross-tenant key fallback is allowed.
- The readiness endpoint MUST remain unavailable when startup key validation
  fails, preventing APIM or Container Apps from routing traffic to the instance.
- Issued tokens MUST use an explicit algorithm allowlist and include verified
  `iss`, `aud`, `iat`, and `exp` values plus the canonical `tenant_id`.
- Algorithm choice MUST be server-controlled. Tokens using `none`, an unexpected
  algorithm, or a key type inconsistent with the configured algorithm MUST be
  rejected.
- External and internal session-token paths MUST apply the same tenant-key
  selection and fail-closed behavior.

Sources: OWASP JSON Web Token for Java Cheat Sheet and REST Security Cheat Sheet;
Quarkus SmallRye JWT guidance as the closest framework-specific fallback.

### JWT verification and temporary PNPG fallback

- Signature, allowed algorithm, issuer, audience, and expiration MUST be
  validated before trusting `tenant_id` or any other token claim.
- Verification-key selection MUST use the trusted tenant resolved from the
  browser URL by APIM to select the corresponding tenant JWKS, then use JWT
  `kid` to select the candidate key.
- The selected `kid` MUST exist in the allowed key set for that tenant. Unknown
  or cross-tenant key identifiers MUST be rejected before claims are trusted.
- `tenant_id` and issuer claims MUST be used only after successful signature
  verification.
- A missing `tenant_id` MUST be rejected by default.
- The `PNPG` fallback MAY be applied only after the token has been verified as a
  genuine `hub-spid-login` token using its configured issuer.
- A verified `hub-spid-login` token without `tenant_id` MUST be accepted only
  when the APIM-set header is exactly `PNPG`.
- A present tenant claim MUST never be overwritten by the fallback. Any
  header/claim mismatch MUST be rejected.
- The compatibility branch SHOULD be feature-gated and instrumented so it can
  be removed without changing the strict validation path.

### Dashboard JWT exchange

- `dashboard-bff` MUST include `tenant_id` in every generated exchange token.
- The exchange-token tenant MUST come from the validated server-side tenant
  context established from the browser URL and authenticated session.
- `dashboard-bff` MUST NOT trust or blindly copy an unverified tenant claim from
  the source token.
- For the temporary PNPG compatibility flow, the exchange token MUST contain
  `tenant_id=PNPG` only after the source token and the trusted PNPG request
  context have passed the documented validation.
- Exchange tokens MUST retain the configured issuer, audience, expiration,
  signature algorithm, and `kid` controls already required for session JWTs.

Sources: OWASP JSON Web Token for Java Cheat Sheet, Authentication Cheat Sheet,
and Authorization Cheat Sheet.

### Tenant authorization and propagation

- Tenant consistency is an authorization decision, not optional request
  metadata. It MUST be enforced before protected business logic or data access.
- Downstream calls MUST propagate the tenant held in trusted server-side request
  context. They MUST NOT forward an arbitrary inbound header independently of
  the validated context.
- Asynchronous work or execution outside the original request context MUST carry
  an explicitly validated tenant value; it MUST NOT silently select a default.
- Services reachable through paths other than APIM MUST authenticate their
  caller and independently verify the JWT/header relationship.
- RBAC, ABAC, ReBAC, or other authorization beyond tenant isolation is out of
  scope for SELC-9170; no permissions model may be inferred from tenant identity.

Sources: OWASP Authorization Cheat Sheet and OWASP Proactive Controls:
Enforce Access Controls.

### Secret and key management

- Private signing keys, OneIdentity credentials, JWTs, and Key Vault secret
  values MUST NOT be committed, embedded in Terraform values, returned in
  errors, or written to logs.
- Container Apps MUST consume private keys through Key Vault secret references
  using managed identity. The identity MUST have only the permissions required
  to read the specific runtime secrets.
- Tenant keys MUST use distinct secret names and configuration bindings to
  prevent accidental aliasing between tenants.
- Rotation MUST support a bounded verification overlap between old and new
  public keys without allowing new tokens to be signed by a retired key. The
  published certificate MUST have a distinct `kid`; the overlap duration is
  `TO BE DECIDED`.
- The JWKS update process MUST support explicit key removal before revocation is
  considered complete. Updating Key Vault alone is insufficient while the old
  public key remains published or cached.
- Every JWKS publication or removal MUST purge the `.well-known/jwks.json` path
  on the corresponding tenant CDN.
- A certificate MUST NOT be published to another tenant's JWKS or CDN storage.
- Revoked or compromised keys MUST be removable without rebuilding the
  application image.
- Consolidation into `infra/cert/dev-ar` MUST create distinctly named
  replacement resources with `_ar` and `_pnpg` suffixes.
- Legacy PNPG certificates MUST remain exclusively owned by `dev-pnpg` until
  cutover is validated, then be removed through that original state.
- The same Azure certificate resource MUST NOT be imported into or managed by
  both Terraform states.

Sources: `infra/resources/_modules/container_app_microservice`,
`infra/core/_modules/key_vault`; fallback to OWASP Secrets Management and
Cryptographic Storage Cheat Sheets.

### Input validation, errors, and logging

- Tenant IDs, issuer values, audience values, and key identifiers MUST be
  checked against server-side allowlists and bounded before use.
- Tenant and token-validation failures MUST return a generic RFC 7807-style 4xx
  response and MUST NOT reveal known tenants, key names, issuer-selection logic,
  or cryptographic failure details.
- Missing headers, unknown tenants, header/claim mismatches, and invalid PNPG
  fallback requests MUST return `400 Bad Request`; disabled authentication
  tenants MUST return `403 Forbidden`.
- Missing or invalid server-side signing configuration MUST fail startup and be
  visible in operational logs without exposing key material. It does not produce
  a request-level client response because the container is not ready.
- Audit logs MUST record tenant rejection, header/claim mismatch, missing-key
  failure, and use of the temporary PNPG fallback with a correlation identifier.
- Logs MUST NOT contain raw JWTs, private keys, credentials, SAML assertions, or
  unnecessary personal data. User-controlled values MUST be sanitized before
  logging to prevent log injection.
- Repeated authentication failures and fallback use SHOULD be observable and
  alertable without exposing sensitive payloads.

Sources: OWASP Input Validation, Error Handling, and Logging Cheat Sheets.

### Deployment and CI/CD

- Terraform changes affecting APIM tenant mapping, Container App ingress,
  managed identity, or Key Vault bindings MUST receive review as security
  boundary changes.
- Deployment pipelines MUST scan source and generated artifacts for secrets and
  MUST prevent private keys from entering container images, build logs, or
  Terraform state.
- Dependency and container-image vulnerability checks MUST block known
  unpatched critical vulnerabilities.
- Production key or tenant configuration changes MUST use the existing
  environment approval controls; no manual out-of-band secret distribution is
  permitted.
- Rollout of tenant-specific signing MUST preserve verification of sessions
  issued before rotation or cutover for only the explicitly approved overlap
  period.

Sources: `infra/resources/auth`,
`infra/resources/_modules/container_app_microservice`; fallback to OWASP CI/CD
Security and Docker Security Cheat Sheets.

## Selected Manicode Prompts

- `Code quality -> OWASP Proactive Controls (fallback; Code Quality/00 General Code Quality Prompts is not present)`
- `API security -> OWASP REST Security Cheat Sheet + OWASP API Security Top 10 (fallback; Web and API Security/06 Secure API Developer is not present)`
- `JWT security -> OWASP JSON Web Token for Java Cheat Sheet (fallback; no local JWT prompt directory)`
- `HTTP boundary and CORS -> OWASP CORS Cheat Sheet + infra/resources/_modules/apim_api (fallback; no local CORS prompt directory)`
- `Rate limiting -> OWASP Denial of Service Cheat Sheet + OWASP API4:2023 (fallback; no local rate-limiting prompt directory)`
- `Backend framework -> Quarkus SmallRye JWT and Quarkus Security guidance (fallback; no Quarkus prompt directory)`
- `Authentication and session model -> Azure API Management under infra/resources/_modules/apim_api and infra/resources/auth + OWASP Authentication and JWT Cheat Sheets`
- `Authorization model -> Tenant consistency only, using OWASP Authorization Cheat Sheet; RBAC/ABAC/ReBAC is out of scope for SELC-9170`
- `Secret and certificate management -> infra/cert, infra/core/_modules/key_vault, and infra/resources/_modules/container_app_microservice + OWASP Secrets Management and Key Management Cheat Sheets`
- `Deployment and infrastructure -> infra/resources/auth and infra/resources/_modules/container_app_microservice + OWASP CI/CD Security and Docker Security Cheat Sheets`
- `Client framework -> React: OWASP Cross Site Scripting Prevention and Content Security Policy Cheat Sheets (fallback; no React prompt directory)`
