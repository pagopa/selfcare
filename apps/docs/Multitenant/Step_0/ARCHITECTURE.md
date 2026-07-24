# Multitenant Architecture

## Required Architecture Inputs

- Requirements source: REQUIREMENTS.md (`apps/docs/Multitenant/REQUIREMENTS.md`)
- System purpose: Enable a single backend deployment to serve multiple tenants (`selfcare.pagopa.it`, `imprese.notifichedigitali.it`) identified per request, replacing today's per-tenant deployments (REQUIREMENTS.md "Project purpose").
- Primary use cases: Tenant-aware request routing through APIM; tenant claim issuance during login (OneIdentity/`auth` and `hub-spid-login`); tenant-consistent authorization on every backend call (REQUIREMENTS.md SELC-1..SELC-7).
- Target users / actors: End users of `selfcare.pagopa.it` (OneIdentity login) and `imprese.notifichedigitali.it` (`hub-spid-login`); backend microservices under `apps/`; APIM as the shared gateway (REQUIREMENTS.md "Primary users / actors").
- Runtime environment: Azure Container Apps, one shared Container App Environment intended to host the multitenant backend (`infra/core/_modules/container_app_environments`, `infra/resources/_modules/container_app_microservice`).
- Server framework: Quarkus 3.31.x on Java 17, reactive (SmallRye Mutiny), per repository convention; no change implied by REQUIREMENTS.md.
- Client framework: TO BE DECIDED — frontends for both tenants are not part of this repository; no source found under `apps/` or `/infra/`.
- API style and integration model: REST over HTTP, fronted by Azure API Management (`infra/resources/_modules/apim_api`, `infra/resources/_modules/apim_external_api`); JAX-RS/RESTEasy Reactive controllers per app; OpenAPI schemas generated per service (repository convention).
- Authentication and session model: Two JWT-issuing flows — OneIdentity → `auth` microservice (custom claims supported today) and `hub-spid-login` (opaque token issuance, no custom claims today); APIM currently holds separate JWT-validation certificates per tenant (`infra/core/_modules/apim/jwt.tf`: `jwt-spid-crt` for AR, `jwt-pnpg-spid-crt` for PNPG). Target model per REQUIREMENTS.md SELC-2/SELC-3/SELC-4: both flows MUST produce a JWT carrying a tenant claim, defaulting to `PNPG` when `hub-spid-login` omits it (SELC-3.1).
- Data model expectations: MongoDB via Panache reactive repositories (repository convention); REQUIREMENTS.md does not mandate tenant-scoped data partitioning, only tenant-aware request handling (SELC-7.1/7.2); per-microservice data partitioning is explicitly TO BE DECIDED (SELC-7.3).
- Deployment model: Terraform-defined, environment-suffixed stacks (`-ar` for selfcare.pagopa.it, `-pnpg` for imprese.notifichedigitali.it) across dev/uat/prod, one `container_app_microservice` module invocation per app per environment (`infra/resources/<app>/{dev,uat,prod}-{ar,pnpg}`). This is today's per-tenant deployment topology that the requirement asks to consolidate; the target shared-deployment topology is TO BE DECIDED.
- Scale expectations: TO BE DECIDED — not specified in REQUIREMENTS.md or `/infra/`.
- Security expectations: JWT bearer validation at APIM and at each service (`mp.jwt.verify.publickey`, repository convention); tenant claim MUST be consistent with the `X-Tenant-Id` header on every request, with hard rejection on mismatch (SELC-2.3); no silent fallback to a default tenant for the header (SELC-1.3), but a defined default (`PNPG`) for the missing-claim case on `hub-spid-login` tokens only (SELC-3.1/SELC-3.4).

## Initial Architecture (Provisional)

**Assumption A**: The existing per-app Quarkus service boundaries (`apps/<service>`) are preserved; multitenancy is added as a cross-cutting concern within each service, not as a service split or a new per-tenant service.

**Assumption B**: APIM remains the single ingress point for both tenant hostnames and is responsible for the first tenant-resolution step, per SELC-5.

1. **Tenant resolution at the edge (APIM)** — APIM inspects the incoming request's host (and/or path, exact signal TO BE DECIDED per SELC-5.4/Open Questions) and resolves a tenant identifier. It injects/overwrites the `X-Tenant-Id` header on every forwarded request (SELC-5.1/5.2). The host/path→tenant mapping source of truth is TO BE DECIDED (SELC-6.3).
2. **Tenant claim issuance at login**:
   - `auth` (OneIdentity flow) adds the tenant claim to the JWT it builds, using the same resolution strategy as APIM (SELC-4).
   - `hub-spid-login` (opaque token issuer) needs an added or wrapping layer to inject the tenant claim (SELC-3); mechanism (post-processing, wrapper, shared issuer) is an **open point**, not decided here.
3. **Tenant enforcement at each backend service** — every service under `apps/` reads `X-Tenant-Id` and the JWT tenant claim on each request, defaults a missing claim to `PNPG` only when the token originates from `hub-spid-login` (SELC-3.1), and rejects the request on any header/claim mismatch or on a missing/unknown header (SELC-1.3, SELC-2.3). This is a shared, cross-cutting concern — candidate for a common library/filter, but no such library is mandated by REQUIREMENTS.md; introducing one is an implementation decision, not fixed here.
4. **Service-to-service propagation** — when one microservice calls another (e.g., BFF → downstream MS), the `X-Tenant-Id` header MUST be forwarded unchanged (SELC-1.2). No new integration pattern is introduced beyond propagating this header.
5. **Deployment consolidation** — the two environment-suffixed deployment stacks (`-ar`, `-pnpg`) per app are collapsed toward a single deployment per environment tier (dev/uat/prod) capable of serving both tenants. The exact rollout/migration approach (big-bang vs. phased/parallel-run) is explicitly **not decided** here (see Open Questions in REQUIREMENTS.md).

**Unknowns kept visible (not guessed):**
- Claim name/format for the tenant claim (Open Question).
- Exact APIM tenant-resolution signal (host vs. path vs. subdomain) and its configuration store (SELC-5.4, SELC-6.3).
- `hub-spid-login` injection mechanism (SELC-3).
- Whether `auth` and `hub-spid-login` converge to one issuing component (Open Question).
- Any tenant-specific data partitioning per microservice (SELC-7.3).
- Client/frontend framework and scale expectations (out of scope for this repository).

## Requirement Traceability

| Architecture element | Requirement group | Notes |
|---|---|---|
| APIM host/path tenant resolution + `X-Tenant-Id` injection | SELC-5, SELC-6 | Resolution signal and mapping source of truth TO BE DECIDED (SELC-5.4, SELC-6.3) |
| `auth` claim issuance | SELC-4 | Depends on shared resolution strategy from SELC-6 |
| `hub-spid-login` claim injection layer | SELC-3 | Injection mechanism is an open point; no design committed |
| Per-service header/claim validation and enforcement | SELC-1, SELC-2 | Mismatch/missing-header handling defined (reject); missing-claim default (`PNPG`) scoped to `hub-spid-login` tokens only |
| Service-to-service header propagation | SELC-1.2 | No new integration pattern; propagate existing header |
| Backend service refactor for tenant context | SELC-7 | Per-app data/config partitioning inventory still needed (SELC-7.3) |
| Deployment consolidation (`-ar`/`-pnpg` → shared) | System purpose, SELC-5.3 | Migration/rollback strategy is an open question, not addressed here |

Requirements needing more architecture input before implementation: SELC-3 (injection mechanism), SELC-5.4 (routing signal/config store), SELC-6.3 (source of truth), SELC-7.3 (per-app data partitioning), and the deployment migration strategy.

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR description.
- Only use libraries that are actively maintained (commit or release within the last 12 months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear security track record.
