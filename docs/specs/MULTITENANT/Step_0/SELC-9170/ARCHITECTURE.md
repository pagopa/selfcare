# SELC-9170 - Remaining Multitenant Authentication Architecture

## Required Architecture Inputs

- `Requirements source: REQUIREMENTS.md`
- `System purpose: Complete multitenant authentication in auth by signing every session JWT with the private key assigned to the resolved tenant.`
- `Primary use cases: Validate all enabled tenant signing keys at startup; issue external and internal auth session tokens with the resolved tenant signing key; select verification keys from the URL-derived tenant and JWT kid; generate dashboard-bff exchange tokens containing the validated tenant.`
- `Target users / actors: Tenant frontend users, auth, hub-spid-login, APIM, and downstream Selfcare microservices.`
- `Runtime environment: Azure API Management in front of the auth service running as an Azure Container App; Azure Key Vault supplies application secrets; MongoDB-compatible Azure Cosmos DB is used by auth for persisted authentication data.`
- `Server framework: Java 17 with Quarkus 3.31.2, RESTEasy Reactive, SmallRye JWT, MicroProfile REST Client, and reactive MongoDB Panache.`
- `Client framework: React for both tenant frontends; the frontend sources are external to this repository.`
- `API style and integration model: HTTPS REST APIs described by OpenAPI, exposed through APIM; APIM resolves the tenant from the browser URL and overwrites X-Tenant-Id; auth calls downstream services through HTTP REST clients; dashboard-bff generates signed JWT exchange tokens for downstream product integrations.`
- `Authentication and session model: auth issues signed JWT sessions containing tenant_id with AR or PNPG values; OneIdentity is used by the currently enabled AR auth flow; hub-spid-login issues independently verified JWTs that may temporarily omit tenant_id and are treated as PNPG only when the trusted header is PNPG. Consumers select an allowed verification key using the trusted URL-derived tenant and JWT kid, then validate tenant_id after signature verification. dashboard-bff exchange tokens also contain the validated tenant_id. Fine-grained authorization beyond tenant consistency is out of scope for SELC-9170.`
- `Data model expectations: No new persistent business entity is required. Tenant definitions and tenant-specific authentication material are runtime configuration; existing authentication and OTP records remain tenant-scoped. Private signing material and certificates are managed in Azure Key Vault by infra/cert; each tenant's public verification keys are distributed as kid-identified entries in the JWKS file hosted by that tenant's CDN storage.`
- `Deployment model: Terraform provisions APIM and one auth Azure Container App for each environment tier, using managed identity to reference Key Vault secrets. In the current rollout the existing AR auth deployment remains the sole auth deployment and only AR is enabled; PNPG remains on the separate hub-spid-login flow and does not require an auth deployment.`
- `Scale expectations: Azure Container Apps scaling remains bounded by configured minimum and maximum replicas; production should use at least two replicas for availability, cap maximum replicas to protect downstream connection pools, and prefer concurrent-request scaling. APIM applies tenant-aware throttling. Exact thresholds remain TO BE DECIDED from APIM analytics.`
- `Security expectations: APIM is the trusted source of X-Tenant-Id and rejects unknown hosts; auth validates every enabled tenant signing key at startup and does not become ready if any key is missing or invalid; tenant signing keys remain isolated in Key Vault; verification certificates are published to the JWKS hosted by the corresponding tenant CDN; validated JWT tenant identity must match the trusted header, except for the explicit verified hub-spid-login missing-claim fallback to PNPG. Header/claim mismatches return 400 RFC 7807 Problem responses. Rotation overlap duration and regulatory or privacy constraints are TO BE DECIDED.`

## Initial Architecture (Provisional)

**Assumption 1:** The Step 0 APIM host-to-tenant mapping and unconditional
`X-Tenant-Id` overwrite remain the trusted tenant-resolution boundary.

**Assumption 2:** The existing `auth` tenant registry remains the runtime source
for enabled tenants and tenant-specific authentication configuration.

**Assumption 3:** The configured JWT issuer is read only after cryptographic
verification and identifies whether the token originated from
`hub-spid-login`.

1. **Tenant-specific signing configuration**  
   Extend the tenant configuration boundary so each tenant enabled for `auth`
   resolves to its own session-token signing key. Key values are supplied as
   Key Vault-backed Container App secrets and are not stored in the tenant
   registry JSON or application source.

2. **Fail-closed token issuance**  
   At startup, `auth` loads and validates the signing key for every enabled
   authentication tenant. Missing or invalid material fails application startup,
   so the Container App does not become ready or serve requests. Once started,
   the session-token issuer obtains the current tenant from the established
   request context and signs both external and internal session JWTs with that
   tenant's validated key.

3. **Existing JWT verification boundary**  
   APIM resolves the expected tenant from the browser URL. Consumers use that
   trusted tenant to select the JWKS served through that tenant's CDN, then use
   the JWT `kid` to select a candidate key from that set. Only after signature
   verification may they trust `tenant_id` or issuer provenance. For an `auth`
   token, `tenant_id` is required and must equal `X-Tenant-Id`. For a verified
   `hub-spid-login` token only, absence of `tenant_id` produces the temporary
   effective tenant `PNPG`; the request is accepted only when `X-Tenant-Id` is
   also `PNPG`.

4. **Existing tenant propagation boundary**  
   Outbound service-to-service REST calls made in an authenticated request
   propagate the trusted tenant value from server-side request context. They do
   not derive tenant identity from caller-controlled input or independently
   default it.

5. **Key lifecycle boundary**  
   `infra/cert` remains the lifecycle automation boundary. Its `jwt_keys`
   modules generate certificates and store private material in Key Vault. A
   Terraform-triggered process publishes each certificate, identified by
   `kid`, to `.well-known/jwks.json` in the Azure Storage account behind the
   corresponding tenant CDN and purges that CDN after publication. Rotation
   keeps existing entries in that tenant's JWKS during an overlap period.
   Revocation requires the consolidated automation to remove the affected entry
   and purge the corresponding CDN; the current append-only JWKS script must be
   extended for this behavior.

6. **Development certificate consolidation**  
   The consolidated `infra/cert/dev-ar` configuration creates new certificate
   resources whose names end in `_ar` or `_pnpg`. Because these have distinct
   Azure resource identities, they are owned only by the `dev-ar` Terraform
   state. Existing PNPG certificates remain owned by `dev-pnpg` during cutover
   and are destroyed from that state only after consumers use and validate the
   new suffixed certificates. No resource is imported into both states.

7. **Dashboard token exchange boundary**  
   `dashboard-bff` generates JWT exchange tokens with its configured exchange
   signing key and `kid`. Before generation, it consumes the tenant already
   validated for the browser request and writes that value to `tenant_id` in the
   exchange token. It must not rely on copying an absent or unverified tenant
   claim from the source token.

**Unknowns kept visible:**

- rotation overlap duration and revocation approval;

- removal date and migration path for the temporary `PNPG` fallback;
- exact rate limits and production replica bounds derived from APIM analytics.

## Requirement Traceability

| Architecture component or boundary | Requirement group | Traceability |
| --- | --- | --- |
| Tenant-specific signing configuration | SELC-2.2, SELC-2.3 | Associates each enabled tenant with isolated signing material. |
| Startup signing-key validation | SELC-2.4, SELC-2.5 | Prevents the container from becoming ready with incomplete or invalid tenant key configuration. |
| Existing JWT verification and PNPG compatibility boundary | Step 0 prerequisite | Already reconciles verified claims or the permitted fallback with the APIM-set header. |
| Existing tenant propagation boundary | Step 0 prerequisite | Already carries the validated tenant through downstream authenticated calls. |
| Key lifecycle boundary | SELC-2.2 | Keeps signing material external to the application and supports continued verifiability. |
| `infra/cert` certificate lifecycle and tenant JWKS publication | SELC-2.6 to SELC-2.9 | Generates and stores private material in Key Vault, publishes each tenant's verification certificates to its CDN storage, and defines rotation/revocation behavior. |
| Consolidated development certificate environment | SELC-2.10 to SELC-2.13 | Creates tenant-suffixed replacement certificates in `dev-ar`, cuts consumers over, then removes legacy PNPG resources from their original state. |
| URL-derived tenant and `kid` key selection | SELC-3.1 to SELC-3.3 | Constrains verification before any JWT tenant claim is trusted. |
| `dashboard-bff` token exchange | SELC-3.4, SELC-3.5 | Adds the validated request tenant to every generated exchange token. |

Requirements needing more architecture input:

- **SELC-2.9:** rotation overlap duration and revocation approval process.

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR description.
- Only use libraries that are actively maintained (commit or release within the last 12 months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear security track record.
