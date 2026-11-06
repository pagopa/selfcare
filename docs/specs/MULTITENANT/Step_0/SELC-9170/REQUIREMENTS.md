# SELC-9170 - Remaining Multitenant Authentication Requirements

## Context

Step 0 already provides:

- tenant resolution in APIM from the incoming host and trusted propagation
  through the `X-Tenant-Id` header;
- validation in `auth` of missing, unknown, or disabled tenants;
- the `AR` and `PNPG` tenant registry;
- tenant-specific OneIdentity client credentials;
- the `tenant_id` claim, with values `AR` or `PNPG`, in session JWTs issued by
  `auth`;
- tenant scoping of the authentication and OTP flows;
- temporary resolution of verified `hub-spid-login` tokens without `tenant_id`
  as `PNPG`, with `400 Bad Request` on header mismatch;
- header/claim reconciliation across authenticated backend requests;
- propagation of the validated tenant on service-to-service calls.

The remaining work concerns tenant-specific JWT signing keys.

As a temporary compatibility rule, tokens issued by `hub-spid-login` will not
contain the `tenant_id` claim. For those tokens only, the tenant is treated as
`PNPG` during validation.

Only the `AR` tenant is currently enabled for the `auth` authentication flow.
In the current rollout, `PNPG` continues to authenticate through
`hub-spid-login`; enabling `PNPG` in `auth` is not required.

## Required Requirement Inputs

- `Project purpose: Complete multitenant authentication in auth by signing session JWTs with the private key assigned to the resolved tenant.`
- `Primary users / actors: Tenant frontend users, auth, and downstream JWT consumers.`
- `Core workflows: Resolve the enabled auth tenant, select its signing key, issue the session JWT, and reject issuance when tenant-specific key material is unavailable.`
- `Business objects / data entities: Tenant identifier, tenant-specific JWT-signing private key, signing certificate, verification key, and JWT session token.`
- `External integrations: OneIdentity, hub-spid-login, Azure API Management, Azure Key Vault, and the public JWKS endpoint.`
- `Authentication / roles: OneIdentity/auth and hub-spid-login authentication flows; tenant consistency is enforced as an authorization boundary. Fine-grained application authorization is out of scope for SELC-9170.`
- `Regulatory or privacy constraints: TO BE DECIDED`

## Functional Requirements

### 2. Tenant-specific JWT signing

- **SELC-2.2** `auth` MUST sign each JWT session token with the private key
  configured for the resolved tenant.
- **SELC-2.3** `auth` MUST NOT use one tenant's JWT-signing private key to issue
  a session for another tenant.
- **SELC-2.4** During startup, `auth` MUST validate the signing key configured
  for every enabled authentication tenant.
- **SELC-2.5** If an enabled tenant signing key is missing or invalid, `auth`
  MUST fail startup and the container MUST NOT become ready. No request-level
  error response is expected because the application cannot serve traffic.
- **SELC-2.6** Tenant signing certificates and private keys MUST be generated,
  stored, and rotated through the Terraform automation under `infra/cert`.
- **SELC-2.7** Private signing material MUST be stored in Azure Key Vault and
  MUST NOT be distributed through the public verification channel.
- **SELC-2.8** Each tenant's verification certificates MUST be published with
  distinct key identifiers in the `.well-known/jwks.json` file hosted in the
  storage account exposed by that tenant's CDN URL.
- **SELC-2.9** Rotation MUST preserve the previous verification key for an
  explicitly controlled overlap period. Revocation MUST remove the affected key
  from the tenant's managed JWKS and purge that tenant's CDN cache.
- **SELC-2.10** The `infra/cert` configuration MUST manage the tenant-specific
  development certificates together from `infra/cert/dev-ar`.
- **SELC-2.11** Each consolidated certificate resource MUST have a tenant suffix
  in its name: `_ar` for AR and `_pnpg` for PNPG.
- **SELC-2.12** The suffixed certificates in `dev-ar` MUST be created as new,
  distinctly named resources. Legacy certificates MUST remain owned only by
  their existing Terraform state until consumers have switched to the new
  certificates.
- **SELC-2.13** After cutover validation, legacy PNPG certificate resources MUST
  be removed through the existing `dev-pnpg` Terraform state. The same Azure
  resource MUST NOT be imported into or managed by both states.

### 3. Verification key selection and token exchange

- **SELC-3.1** APIM MUST resolve the expected tenant from the browser URL and
  propagate it as the trusted `X-Tenant-Id` request context.
- **SELC-3.2** Token consumers MUST use the trusted URL-derived tenant together
  with the JWT `kid` header to select an allowed verification key from the
  corresponding tenant JWKS. They MUST NOT trust the JWT `tenant_id` claim
  before signature verification.
- **SELC-3.3** After signature verification, token consumers MUST verify that
  the JWT `tenant_id` claim matches the trusted URL-derived tenant, except for
  the documented temporary `hub-spid-login` fallback.
- **SELC-3.4** Every JWT exchange token generated by `dashboard-bff` MUST contain
  `tenant_id`, using the tenant already validated for the browser request.
- **SELC-3.5** `dashboard-bff` MUST NOT derive the exchange-token tenant from an
  unverified request value. For a PNPG source token without `tenant_id`, it MUST
  use the validated `PNPG` request tenant established by the compatibility
  flow.

### 4. Shared JWT validation libraries

- **SELC-4.1** The shared JWT validation libraries for Quarkus and Spring
  applications MUST implement the same tenant-validation behavior.
- **SELC-4.2** The libraries MUST determine the token tenant only after
  validating the JWT signature, allowed algorithm, issuer, audience, and
  expiration.
- **SELC-4.3** For a token cryptographically verified as issued by the configured
  SPID issuer (`hub-spid-login`), if `tenant_id` is absent, the libraries MUST
  assign the effective token tenant `PNPG`.
- **SELC-4.4** If a verified SPID token contains `tenant_id`, the libraries MUST
  validate and preserve that value and MUST NOT replace it with the `PNPG`
  default.
- **SELC-4.5** The libraries MUST compare the effective token tenant with the
  trusted `X-Tenant-Id` request header. A missing trusted header or a tenant
  mismatch MUST cause authentication to fail with `400 Bad Request` using the
  RFC 7807 `Problem` convention.
- **SELC-4.6** The `X-Tenant-Id` header MUST be considered trusted only when it
  is derived from the request URL and overwritten by APIM, or supplied through
  an equivalent authenticated gateway-to-backend channel. A client-provided
  header MUST NOT independently establish tenant identity.
- **SELC-4.7** After successful validation, the libraries MUST expose the
  reconciled tenant through a trusted application security context so business
  logic and downstream calls do not independently interpret the raw header or
  unverified JWT claims.

## Open Questions

- What overlap duration applies during certificate rotation, and what approval
  triggers removal of the previous key from JWKS?
