# FN_VPN — Security

Inputs: [REQUIREMENTS.md](./REQUIREMENTS.md), [ARCHITECTURE.md](./ARCHITECTURE.md). Scope: onboarding Function Apps (`infra/resources/_modules/functions`, dev/uat/prod × ar/pnpg) moved behind private endpoint + VPN.

> **Prompt library status:** the security prompt library (`Web and API Security/`, `Code Quality/`, … with `PROMPT.md`) was not found locally or in this repo. Library slots fall back to public OWASP sources (named per slot). Infra/stack slots point to repository paths. Re-resolve when the library location is known.

## Required Security Inputs

- `Exposure: Function App (app + SCM) — inbound only via private endpoint from shared VNet and P2S VPN; public network access disabled (SELC-1.1–1.5)`
- `Callers: onboarding-ms/bff/cdc on Azure Container Apps, APIM (Internal VNet mode), support scripts/operators on VPN, GitHub Actions deploy (SELC-2, SELC-4.2)`
- `AuthN (service): function key, all 12 HTTP triggers authLevel=FUNCTION; key in Key Vault secret fn-onboarding-primary-key (SELC-2.5)`
- `AuthN (human/network): Azure VPN Gateway P2S, OpenVPN, Entra ID; access = vpn.members per environment (SELC-1.6)`
- `AuthZ: Azure RBAC via <prefix>-adgroup-* groups; no application-level authorization change in scope`
- `Secrets: Azure Key Vault (app settings via @Microsoft.KeyVault references, managed identities)`
- `Data: personal data of institution representatives (GDPR); data model unchanged`
- `CI/CD: GitHub Actions with OIDC (id-token: write); infra on self-hosted VNet runners; code deploy runner TO BE DECIDED (ARCHITECTURE U3)`
- `Rate limiting / CORS / WAF for Function App: TO BE DECIDED (not stated in requirements)`
- `AI / agent components: none`

## Selected Prompts

- `Code quality -> UNRESOLVED in library; fallback OWASP Top 10 Proactive Controls + repo conventions /apps/onboarding-functions`
- `API security -> UNRESOLVED in library; fallback OWASP API Security Top 10 (API2 Broken Authentication, API8 Security Misconfiguration, API9 Improper Inventory Management)`
- `Backend framework -> Quarkus (Quarkus Azure Functions) /apps/onboarding-functions`
- `Authentication, Authorization (service) -> API Management Azure /infra/resources/_modules/apim_external_api (+ /infra/resources/_modules/apim_api, pattern used by /infra/resources/webhook)`
- `Authentication (network/human) -> VPN Entra ID /infra/core/_modules/vpn`
- `Authorization model -> Azure RBAC /infra/core/_modules/roles (groups <prefix>-adgroup-*)`
- `Network / private access -> /infra/resources/_modules/functions, /infra/core/_modules/dns_private, /infra/core/_modules/networking`
- `Secret management -> Azure Key Vault /infra/core/_modules/key_vault (+ /infra/resources/_modules/functions function.tf)`
- `Container platform -> Azure Container Apps /infra/resources/_modules/container_app_microservice, /infra/core/_modules/container_app_environments`
- `Terraform -> /infra/core/*, /infra/resources/*`
- `CI/CD -> /.github/workflows/call_release_functions.yml, /.github/workflows/call_release_infra.yml, /infra/bootstrap/_modules/github_runner`
- `Logging -> UNRESOLVED in library; fallback OWASP Logging Cheat Sheet + Application Insights config in /infra/resources/_modules/functions`
- `WAF / rate limiting / CORS -> TO BE DECIDED`
- `Client framework -> not applicable (no UI)`

## Provisional Security Rules

### 1. Network boundary — `/infra/resources/_modules/functions`, `/infra/core/_modules/dns_private`
- N1. Function App MUST have public network access disabled for both app and SCM; no IP allow-list exceptions to the public internet.
- N2. Inbound MUST only use the private endpoint (`sites`) in the shared VNet; do not add service endpoints or access restrictions as a substitute.
- N3. `privatelink.azurewebsites.net` MUST contain A records for `<app>` and `<app>.scm`, linked only to internal VNets; the public hostname stays unchanged.
- N4. Do not open NSG rules or peering wider than needed for Container Apps, APIM, VPN pool (`172.16.1.0/24`) and runner subnet.
- N5. Outbound MUST stay through VNet integration + NAT Gateway; do not change egress IP (external allow-lists depend on it).
- N6. Verification for each env MUST include a negative test: request from a public IP fails (SELC-1.1, 2.7).

### 2. Service authentication — `/infra/resources/_modules/apim_external_api`, `/apps/onboarding-functions`
- A1. HTTP triggers MUST keep `authLevel = AuthorizationLevel.FUNCTION`; never `ANONYMOUS`. Private networking is defense in depth, not a replacement (SELC-2.5).
- A2. Callers MUST send the key in `x-functions-key` header, never in query string (keys in URLs leak into logs).
- A3. APIM policies currently interpolate the function key literally via `templatefile(FN_KEY)`; new or changed policies SHOULD use an APIM named value backed by Key Vault instead of embedding the secret. Fallback: OWASP API2.
- A4. APIM MUST keep `set-header X-FUNCTIONS-KEY exists-action="override"` so client-supplied keys are never forwarded.
- A5. Rotation of `fn-onboarding-primary-key` MUST update Key Vault and all consumers (onboarding-ms, -bff, -cdc, APIM) in the same change.

### 3. VPN access — `/infra/core/_modules/vpn`
- V1. P2S MUST stay Entra ID–authenticated (OpenVPN, `aad_*`); no certificate or RADIUS fallbacks.
- V2. VPN access MUST be granted only via `vpn.members` of the environment config (`selc-d|u|p`); remove users on offboarding.
- V3. Prod VPN gateway diagnostics MUST keep flowing to the security Log Analytics workspace and storage (`env_short == "p"`).
- V4. Support scripts MUST NOT store function keys in source; read them from Key Vault at runtime.

### 4. Secret management — `/infra/core/_modules/key_vault`
- S1. App settings with secrets MUST use `@Microsoft.KeyVault(SecretUri=…)` references; no plaintext secrets in `.tf` or workflow files.
- S2. Function and storage access MUST use managed identities; Key Vault policy for the Function App stays `Get` on secrets only.
- S3. Terraform state containing secret values (e.g. key export, APIM policy) MUST stay in the restricted backend; never print secrets in plan output.

### 5. CI/CD — `/.github/workflows/call_release_functions.yml`, `/infra/bootstrap/_modules/github_runner`
- C1. Code deploy (`quarkus:deploy` → SCM) MUST run on runners with private network access; GitHub-hosted runners will fail once SCM is private (ARCHITECTURE U3).
- C2. Keep OIDC login (`id-token: write`) with no long-lived Azure credentials in secrets.
- C3. Pin third-party actions by commit SHA; `pagopa/dx/actions/csp-login@main` is unpinned and SHOULD be pinned.
- C4. Infra apply MUST keep running on `self-hosted` runners scoped per environment label.

### 6. Backend (Quarkus Functions) — `/apps/onboarding-functions`
- B1. No code change is required by this feature; do not alter function signatures, routes or auth levels.
- B2. Keep existing input sanitization conventions (OWASP Java Encoder) for user-controlled strings.
- B3. Error responses MUST NOT expose stack traces, keys or internal hostnames. Fallback: OWASP Proactive Controls C10.

### 7. Logging — Application Insights
- L1. Do not log function keys, `x-functions-key` headers or Key Vault secret values (APIM traces, function logs).
- L2. Personal data (GDPR) MUST NOT be added to logs introduced by this change.
- L3. Rejected public access attempts SHOULD be observable (Function App/App Service access logs). Destination: TO BE DECIDED.

### 8. Rollout safety — `/infra/resources/onboarding-functions/*`
- R1. Per environment: create private endpoint + DNS records, verify all callers (SELC-2) and VPN, then disable public access.
- R2. Prod changes MUST run inside the agreed maintenance window (SELC-5.4).

### UNRESOLVED / TO BE DECIDED
- Prompt library location (all `UNRESOLVED in library` slots).
- Code-deploy runner (C1 / ARCHITECTURE U3).
- Rate limiting, CORS, WAF in front of the Function App.
- Log destination for rejected access (L3).
- GDPR-specific constraints beyond "no new personal data in logs".
