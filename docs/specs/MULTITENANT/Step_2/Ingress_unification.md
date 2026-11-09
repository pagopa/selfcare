# Single Ingress: One Application Gateway Listener and One APIM API per BFF

This document completes Step 2 for the ingress layer. Step 2 consolidates the backend into one Container App
Environment per environment; this document defines how both tenants reach it through **one Application Gateway
listener**, **one APIM API per BFF/service**, and **one API host**, while the tenant is still resolved per
request and the correct tenant registry entry is activated.

Requirements: `SELC-19` in [REQUIREMENTS.md](REQUIREMENTS.md). Security rules: "Single ingress" in
[SECURITY.md](SECURITY.md).

## Current state

| Layer | AR | PNPG |
|---|---|---|
| Frontend | `selfcare.pagopa.it` (`dev.`/`uat.` prefixes) | `imprese.notifichedigitali.it` (dev: `pnpg.dev.selfcare.pagopa.it`) |
| API host | `api.<env>.selfcare.pagopa.it` | `api-pnpg.<env>.selfcare.pagopa.it` |
| BFF paths | `/dashboard`, `/onboarding[/v1]`, `/auth`, `/party-registry-proxy/v1` | `/imprese/dashboard`, `/imprese/onboarding[/v1]`, `/imprese/auth`, `/imprese/party-registry-proxy/v1` |
| Login | `/spid-login/v1` + OneIdentity through `auth` (`auth_enabled = true`) | `/spid/v1` to `hub-spid-pnpg` (`auth_enabled = false`) |
| Application Gateway | listener + URL path map `api` | listener + URL path map `api-pnpg`, backend `hub-spid-pnpg` |
| APIM BFF API | `selc-<e>-api-bff-dashboard` (path `dashboard`) | `selc-<e>-pnpg-api-bff-dashboard` (path `imprese/dashboard`) |
| Tenant in APIM | `apim_api` policy maps `X-Selfcare-Original-Host` to `tenant_hosts` | same |
| Backend | AR Container App Environment | PNPG Container App Environment |

Relevant sources:

- `.github/workflows/env/fe_{dev,uat,prod}.env` and `fe_{dev,uat,prod}_pnpg.env` (`VITE_URL_API_*`);
- `infra/core/_modules/appgateway/main.tf` (listeners, path-based routes, URL path maps, rewrite sets);
- `infra/resources/_modules/local-env/locals.tf` (`tenant_registries`, `tenant_ids`, `tenant_hosts`);
- `infra/resources/_modules/apim_api/main.tf` (host-based tenant policy);
- `infra/resources/_modules/apim_external_api/*` (product-level `tenantId`);
- `libs/selfcare-sdk-security` (`JwtTenantValidator`, `JWTCallerPrincipalFactory`);
- `apps/auth` (`TenantResolutionFilter`, `OidcServiceImpl`, `SessionServiceImpl`).

### How the host is used today

The `http-headers-api` rewrite rule of the Application Gateway sets
`X-Selfcare-Original-Host = {var_host}`. `{var_host}` is an Application Gateway server variable containing the
host requested by the client. The gateway overwrites the header, so a client-supplied value is discarded. APIM
then maps that host to a tenant through `tenant_hosts`. With a single listener and host, this header always
carries `api.<env>…` and no longer identifies the tenant. It remains useful only for tracing.

## Why the gateway cannot route by tenant

- Application Gateway path rules match only the URL path. They cannot route on a header value or on a JWT
  claim, and the gateway does not validate or decode tokens.
- APIM products have no custom metadata fields. A tenant can be attached to a product only through its
  policy (`<set-variable name="tenantId" …/>`) or a named value.

Tenant resolution therefore moves entirely into APIM, and the gateway becomes tenant-agnostic.

## Target tenant resolution in APIM

Trust order, strongest first:

1. **JWT `tenant_id` claim** of the session token issued by `auth`. This is the source of truth for every
   authenticated call. APIM validates the token with the public keys of every tenant before reading the claim.
2. **APIM product `tenantId`** for B2B APIs authenticated by subscription key. Each product policy declares
   `<set-variable name="tenantId" value="AR|PNPG" />`. API and operation policies read it with
   `context.Variables.GetValueOrDefault("tenantId", "<fallback>")` and propagate it as `X-Tenant-Id`, as the
   `tenant_id` claim of the internally minted JWT, and as the `tenantId` query parameter or body field where a
   backend requires it. This is implemented in `infra/resources/_modules/apim_external_api`.
3. **`Origin` header**, only for unauthenticated pre-login operations (login start, token exchange, public
   onboarding). The value is matched exactly against `tenant_registries[*].allowed_origins`. Browsers cannot
   forge `Origin`, but non-browser clients can. This is acceptable only because these operations expose no
   tenant data. The tenant is then sealed into the session token at login.

Policy rule for the BFF APIs:

```text
token present  → validate-jwt (keys of all tenants) → tenantId = claim tenant_id
token absent   → tenantId = tenant registry entry whose allowed_origins contains Origin
tenantId empty or not in the registry → 403 (trace "tenant-audit")
token present and Origin maps to a different tenant → 403 (defence in depth)
set-header X-Tenant-Id (override) = tenantId
```

A client-supplied `X-Tenant-Id` is never forwarded. The backend still reconciles `X-Tenant-Id` with the
JWT claim (Step 0) and builds `TenantContext`, which selects the tenant registry entry (Mongo, storage, JWT
keys) as described in [ARCHITECTURE.md](ARCHITECTURE.md).

## Frontend entry points

The frontend domains stay distinct: they are the pre-login tenant identity and carry the PNPG branding.
Only the API URLs of the PNPG frontends change. The frontends do not send `X-Tenant-Id`.

| Variable in `fe_*_pnpg.env` | From | To |
|---|---|---|
| `VITE_URL_API_AUTH` | `https://api-pnpg.<e>…/imprese/auth` | `https://api.<e>…/auth` |
| `VITE_URL_API_DASHBOARD` | `…api-pnpg…/imprese/dashboard` | `…api…/dashboard` |
| `VITE_URL_API_ONBOARDING` | `…api-pnpg…/imprese/onboarding[/v1]` | `…api…/onboarding/v1` (dev and uat PNPG currently omit `/v1`) |
| `VITE_URL_API_ONBOARDING_V2` | `…api-pnpg…/imprese/onboarding` | `…api…/onboarding` |
| `VITE_URL_API_PARTY_REGISTRY_PROXY` | `…api-pnpg…/imprese/party-registry-proxy/v1` | `…api…/party-registry-proxy/v1` |
| `VITE_URL_API_LOGIN` | `…api-pnpg…/spid/v1` | `…api…/spid/v1` first; then the AR login flow if PNPG moves to `auth` |

The AR `fe_*.env` files do not change. The PNPG URLs change only after the unified APIM APIs are active.

## Application Gateway

File: `infra/core/_modules/appgateway/main.tf`.

1. Keep only the `api` listener. During the transition, `api-pnpg` either uses the same URL path map as `api`
   or returns a `308` redirect to `api.<env>…`, preserving path and query string. The listener, its certificate
   (`app_gateway_api_pnpg_certificate_name`), and its DNS record are removed at the end of the migration.
2. Path-based routing uses only the `api` URL path map:
   - `bff_api` (`/dashboard/*`, `/onboarding/*`, `/party-registry-proxy/*`, `/auth/*`) serves both tenants;
   - `external_api` and `internal_api` are unchanged;
   - a new `${spid_pnpg_path_prefix}/*` rule routes to the `hub-spid-pnpg` backend with
     `rewrite-rule-set-hub-spid-pnpg`. This is moved from the `api-pnpg` map; `/spid/v1` does not collide with
     the AR `/spid-login/v1`;
   - during the transition, `/imprese/*` keeps routing to APIM.
3. Add `/spid/` to the allowed ingress path regular expressions if not already present.
4. In `rewrite-rule-set-apim-headers`, keep `X-Selfcare-Original-Host` for tracing and remove any incoming
   `X-Tenant-Id` (defence in depth; APIM overrides it anyway).

## APIM

### BFF and service APIs (`apim_api`)

1. Add a variable `tenant_resolution = "host" | "token_or_origin"`, default `host` for backward compatibility.
2. `token_or_origin` implements the policy rule above:
   - optional `validate-jwt`, because pre-login operations have no token, using one named value per tenant
     public key;
   - `Origin` lookup on `tenant_ids` (already a list of `{id, origin}`);
   - the existing `403` and `tenant-audit` trace block on failure;
   - API hosts (`api.*`, `api-pnpg.*`) are removed from `allowed_origins`, because they are not browser origins.
3. Keep one API per BFF and service:
   - `dashboard-bff`: keep `apim_api_bff_dashboard` (path `dashboard`) and remove
     `apim_api_bff_dashboard_pnpg` (path `imprese/dashboard`) after the frontend switch;
   - the same applies to `onboarding-bff`, `auth`, and `registry-proxy`
     (`imprese/onboarding`, `imprese/auth`, `imprese/party-registry-proxy`);
   - `private_dns_name` targets the container app in the shared Container App Environment.
4. Deprecate `tenant_hosts` in `local-env/locals.tf` when no API uses `host` resolution. The PNPG `api_uri`
   becomes the AR API host.

### Implemented: tenant from the session-token audience (`aud`)

First step towards `token_or_origin`. It is behind the `apim_api` flag `jwt_tenant_resolution_enabled`, which
defaults to `false` and is wired as `false` in `dashboard-bff`, `onboarding-bff`, `auth` and `registry-proxy`.

- `local-env` adds `jwt_audience` to each tenant registry entry:

  | Env | AR | PNPG |
  |---|---|---|
  | dev | `api.dev.selfcare.pagopa.it` | `pnpg.dev.selfcare.pagopa.it` |
  | uat | `api.uat.selfcare.pagopa.it` | `imprese.uat.notifichedigitali.it` |
  | prod | `api.selfcare.pagopa.it` | `imprese.notifichedigitali.it` |

- `local-env` also exposes `tenant_jwt_audiences = [{id, audience, certificate_id}]`. The certificate ids are the
  APIM certificates `jwt-spid-crt` (AR) and `jwt-pnpg-spid-crt` (PNPG).
- APIM policy when the flag is enabled:

  ```text
  no Bearer token          → existing X-Selfcare-Original-Host resolution
  aud not decodable        → 401 (tenant-audit reason=invalid_jwt)
  aud unknown or ambiguous → 403 (reason=unknown_audience)
  aud → tenant T           → validate-jwt with T's certificate only (signature, exp, aud, iss SPID|PAGOPA)
                             invalid → 401
                             tenant_id claim present and != T → 403 (reason=jwt_tenant_mismatch)
                             tenantId = T (overrides the host-based value)
  set-header X-Tenant-Id (override) = tenantId
  ```

  Each audience is verified only with its own tenant's key, so the policy also binds the key to the tenant.
  The SDK does not provide this binding yet.
- `auth` issues the tenant audience only when `SESSION_TOKEN_TENANT_AUDIENCE_ENABLED=true` (default `false`).
  The audience comes from `jwt.session.audience` in `TENANT_REGISTRY_JSON`. Otherwise `SESSION_TOKEN_AUDIENCE`
  is used, as before. The AR value is unchanged, so the flag only affects PNPG once `auth_enabled = true`.
- Prerequisites before enabling the APIM flag:
  - `hub-spid-login` PNPG must issue `aud` = the PNPG frontend domain. Today it uses `api-pnpg.<env>…`.
  - Verify that `jwt-spid-crt` and `jwt-pnpg-spid-crt` hold the public keys matching the session signing keys
    (`jwt-private-key-pkcs8` for AR in `auth`).
  - Any API that also receives Bearer tokens with a different `aud` (for example machine-to-machine tokens)
    must keep the flag off.

### External APIs (`apim_external_api`)

External APIs are already tenant-aware through product policies. Each AR or PNPG API is linked only to
products of the same tenant. APIs without products (`notification-event`, `external_api_contract`) use the
deployment tenant as the fallback. In the target state, `apim_external_api_ms_v2` and
`apim_pnpg_external_api_ms_v2` merge into one API linked to all products, with a single multi-tenant backend
URL.

## Login: sealing the tenant into the token

Because the session token becomes the primary tenant source, login must bind the tenant to the flow and not
only to a request header:

1. `auth` verifies that the OIDC `redirect_uri` belongs to `tenant_registries[tenant].frontend_uri`. Today
   `OidcServiceImpl.exchange(code, redirectUri)` does not bind `redirectUri` to the tenant.
2. Each tenant uses its own OneIdentity client (`TENANT_<ID>_ONE_IDENTITY_CLIENT_ID`, already modelled in
   `local-env`).
3. Every issuer emits `tenant_id`. `SessionServiceImpl` already does for `auth`. `hub-spid-login` for PNPG does
   not: either extend it or move PNPG to `auth` (`auth_enabled = true` for PNPG).
4. Then set `JWT_TENANT_CLAIM_REQUIRED=true` everywhere. This removes the Step 0 `DEFAULT_TENANT=PNPG`
   fallback (SELC-2.3, SELC-3.1), which is unsafe in a shared Container App Environment.

## Backend prerequisites

- `TENANT_SUPPORTED_TENANTS=AR,PNPG` and a tenant registry containing both tenants in each shared
  container app. Per-tenant secrets need distinct Key Vault secret names, because `container_app_microservice`
  `secrets_names` values must be unique.
- `JWTCallerPrincipalFactory` must bind the verifying key to its tenant, and `JwtTenantValidator` must reject a
  token whose `tenant_id` differs from the tenant owning that key. Today, with several keys and a token without
  `kid`, every key is tried, so a token signed with the AR key and `tenant_id=PNPG` is accepted. Real per-tenant
  `kid` values (JWKS) are preferred; see [JWT_Key_Resolution.md](JWT_Key_Resolution.md).
- Tenant validation must cover non-SPID issuers (machine-to-machine tokens) or document why they are exempt.
- Spring services based on `selfcare-commons` must clear the `ThreadLocal` `TenantContext` after each request
  and propagate it explicitly to asynchronous work.

## Rollout sequence

Applied per environment (DEV, then UAT, then PROD).

| # | Change | Breaking |
|---|---|---|
| 1 | SDK security: key-to-tenant binding | No |
| 2 | `auth` and `hub-spid-login` always emit `tenant_id`; `redirect_uri` bound to the tenant | No |
| 3 | `apim_api`: `tenant_resolution = token_or_origin` on the existing AR and PNPG BFF APIs | No (same result) |
| 4 | Application Gateway: the `api` listener also serves `/spid/*` and `/imprese/*`; `api-pnpg` unchanged | No |
| 5 | Shared Container App Environment; switch the APIM `private_dns_name` | Medium (rollback by repointing) |
| 6 | PNPG frontends: `fe_*_pnpg.env` to `api.<env>…` without `/imprese` | Yes for the frontend (coordinated deploy) |
| 7 | `JWT_TENANT_CLAIM_REQUIRED=true` | Yes if legacy tokens remain valid |
| 8 | `api-pnpg` returns `308`; then remove the listener, certificate, DNS record and `imprese/*` APIs | Yes for external clients |

## Open decisions

1. PNPG login: extend `hub-spid-login` with `tenant_id`, or move PNPG to `auth` and OneIdentity?
2. Pre-login tenant source: `Origin` allowlist, or a tenant path segment only on public endpoints
   (for example `/auth/{tenant}/…`)?
3. Terraform ownership of the unified APIM APIs: the AR stack or a new shared stack?
4. Compatibility window for `api-pnpg.<env>.selfcare.pagopa.it` for external and B2B clients.
5. Dev PNPG domain: `pnpg.dev.selfcare.pagopa.it` or `imprese.dev.notifichedigitali.it`?
6. PNPG product data currently stored in the AR product database must be migrated before the PNPG external
   API product operations target the PNPG `product` deployment.
