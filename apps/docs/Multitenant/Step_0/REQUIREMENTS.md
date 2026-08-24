# Multitenant Architecture for Selfcare Microservices

## Required Requirement Inputs

- Project purpose: Refactor the Selfcare microservices monorepo so that a single backend deployment can serve multiple tenants (currently `selfcare.pagopa.it` and `imprese.notifichedigitali.it`), each identified by a different URL, instead of maintaining separate deployments per environment.
- Primary users / actors: End users authenticating via OneIdentity (selfcare.pagopa.it) or hub-spid-login (imprese.notifichedigitali.it); backend microservices under `apps/` consuming tenant context; APIM as the routing/gateway layer.
- Core workflows: User login and session token issuance (via `auth` or `hub-spid-login`); APIM request routing to a shared backend; tenant resolution and propagation across service-to-service calls; tenant-aware authorization within JWT-secured endpoints.
- Business objects / data entities: Tenant identifier (`X-Tenant-Id`); JWT session token and its claims; APIM incoming host/path used for tenant resolution; backend deployment/environment configuration.
- External integrations: OneIdentity (external IdP), APIM (Azure API Management), `hub-spid-login` (SPID-based login microservice, currently a black box), `auth` internal microservice (token issuance for OneIdentity flow).
- Authentication / roles: Two authentication flows — OneIdentity → `auth` microservice (internal token issuance, custom claims currently supported) and `hub-spid-login` (internal token issuance, black box, custom claims currently NOT supported). Both must converge on a consistent tenant-resolution strategy.
- Regulatory or privacy constraints: TO BE DECIDED

## Functional Requirements

### SELC-1: Tenant Identification via HTTP Header
- SELC-1.1: The system MUST accept and propagate a tenant identifier as an HTTP header (e.g., `X-Tenant-Id`) on every request reaching a backend microservice.
- SELC-1.2: Backend microservices MUST use the `X-Tenant-Id` header value for request routing and for any service-to-service call made on behalf of the originating request.
- SELC-1.3: If the `X-Tenant-Id` header is missing or does not match a known tenant, the system MUST reject the request or respond with an explicit error; silent fallback to a default tenant MUST NOT occur.

### SELC-2: Tenant Identification via JWT Claim
- SELC-2.1: Every JWT session token issued by either authentication flow (OneIdentity/`auth` or `hub-spid-login`) MUST contain a tenant claim identifying the tenant the session belongs to.
- SELC-2.2: The tenant claim MUST remain present and verifiable for the lifetime of the authenticated session, independent of which login mechanism issued the token.
- SELC-2.3: Backend microservices MUST validate that the tenant claim in the JWT is consistent with the `X-Tenant-Id` header on the same request; on mismatch, the system MUST reject the request. If the JWT lacks the tenant claim entirely (e.g., a `hub-spid-login` token per SELC-3.1), validation MUST default the claim value to `PNPG` before performing this consistency check, rather than treating the token as invalid.

### SELC-3: Tenant Claim Injection for `hub-spid-login`
- SELC-3.1: During validation, if a JWT signed by `hub-spid-login` does NOT contain the tenant claim, the system MUST treat it as valid and MUST default the tenant value to `PNPG`.
- SELC-3.4: Even when the tenant claim is defaulted to `PNPG` per SELC-3.1, the `X-Tenant-Id` HTTP header MUST still be present on the request and MUST carry the value `PNPG`; the header is never optional, regardless of whether the claim was present in the token.

### SELC-4: Tenant Claim Injection for OneIdentity / `auth`
- SELC-4.1: The `auth` microservice MUST continue to add the tenant claim to the JWT it builds internally for the OneIdentity login flow.
- SELC-4.2: The tenant value used by `auth` MUST be resolved consistently with the strategy defined in SELC-6, so that the same tenant resolution logic applies across both authentication flows.

### SELC-5: APIM Routing for Shared Backend
- SELC-5.1: APIM MUST resolve the tenant from the incoming request (e.g., host or path) before forwarding it to the shared backend deployment.
- SELC-5.2: APIM MUST inject the resolved tenant identifier into the `X-Tenant-Id` header on every forwarded request.
- SELC-5.3: APIM routing changes MUST preserve existing environment-specific paths for `selfcare.pagopa.it` and `imprese.notifichedigitali.it` from the client's perspective, without requiring frontend changes beyond what is explicitly agreed.
- SELC-5.4: The tenant resolution rule in APIM MUST be defined declaratively (e.g., mapping table of host/path → tenant ID) so new tenants can be onboarded without backend code changes. Exact configuration mechanism: TO BE DECIDED.

### SELC-6: Consistent Tenant-Resolution Strategy
- SELC-6.1: A single, shared tenant-resolution strategy MUST be defined and used identically by APIM, `auth`, and `hub-spid-login` (via its injection mechanism from SELC-3), so that the same incoming request always resolves to the same tenant identifier regardless of authentication flow.
- SELC-6.2: The tenant-resolution strategy MUST cover, at minimum, the two known tenants (`selfcare.pagopa.it`, `imprese.notifichedigitali.it`) and MUST be extensible to additional tenants without requiring changes to this resolution logic itself.
- SELC-6.3: The canonical list of valid tenant identifiers and their mapping to hosts/paths MUST be maintained in a single source of truth accessible to APIM and backend services. Exact storage location/mechanism: TO BE DECIDED.

### SELC-7: Backend Multitenancy Support
- SELC-7.1: Each microservice under `apps/` that currently assumes a single-tenant deployment MUST be refactored to read and honor the tenant context (header and/or JWT claim) on every incoming request.
- SELC-7.2: Tenant-specific configuration or data currently distinguished by separate deployments MUST be resolvable at runtime from the tenant context, without requiring a separate deployment per tenant.
- SELC-7.3: Which specific tenant-dependent configuration/data must be partitioned per microservice: TO BE DECIDED (requires per-app inventory).

## Open Questions

- What is the exact claim name and format for the tenant claim to be added to JWT tokens (e.g., `tenant_id`, `tid`), and must it be namespaced to avoid collisions with existing claims?
- Where and how will the tenant claim be injected into `hub-spid-login` tokens: token post-processing/interception, a wrapping proxy service, or replacing/extending `hub-spid-login` with a shared token-issuing layer also used by `auth`?
- Should `auth` and `hub-spid-login` eventually converge onto a single shared token-issuing component, or remain separate systems that both comply with the same claim contract?
- What is the source of truth for the host/path-to-tenant mapping used by APIM (e.g., APIM named values, a configuration service, a database), and who owns updating it when onboarding a new tenant?
- Should tenant resolution in APIM be based on hostname, URL path prefix, or another signal (e.g., subdomain)? Are there cases where the same host must resolve to different tenants based on path?
- What is the expected behavior when a request's `X-Tenant-Id` header and JWT tenant claim disagree — hard rejection, logging + rejection, or another policy? (Assumed hard rejection in SELC-2.3; confirm.)
- Are there existing tenant-specific business rules, feature flags, or data partitioning (e.g., separate MongoDB databases/collections) per microservice that must be inventoried before refactoring (see SELC-7.3)?
- Do any regulatory/privacy constraints differ between the two tenants (e.g., SPID vs. OneIdentity data handling) that must be preserved in a shared multitenant backend?
- What is the migration strategy and rollback plan for cutting over from two separate deployments to a single multitenant deployment, and is a phased/parallel-run approach required?
- Should tenant identity also be enforced at the data-access layer (e.g., row-level tenant filtering in MongoDB) or is header/claim-level enforcement at the API boundary sufficient?
