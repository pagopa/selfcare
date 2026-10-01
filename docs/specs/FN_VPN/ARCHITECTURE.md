# FN_VPN — Architecture

Source of truth: [REQUIREMENTS.md](./REQUIREMENTS.md). Scope: onboarding Function Apps from `infra/resources/_modules/functions` (dev/uat/prod × ar/pnpg).

## Required Architecture Inputs

- `Requirements source: REQUIREMENTS.md`
- `System purpose: Make the Selfcare onboarding Function App (selc-<env>[-pnpg]-onboarding-fn) reachable only from the internal network and VPN, keeping all functions and callers working (SELC-1..5)`
- `Primary use cases: Internal services and APIM invoke HTTP-triggered functions; support scripts and operators over VPN invoke HTTP functions and SCM/Kudu; GitHub Actions deploys code; timer/queue/orchestration functions keep running`
- `Target users / actors: onboarding-ms, onboarding-bff, onboarding-cdc (Azure Container Apps); APIM external API; support scripts/operators on VPN (vpn.members); GitHub Actions deploy job`
- `Runtime environment: Azure Functions v4, Linux, Java 17, dedicated App Service Plan (B2 dev/uat, P1v3 prod, 1 worker); regional VNet integration on a dedicated subnet with NAT Gateway egress; one shared VNet (vnet_selc_name) for ar and pnpg`
- `Server framework: Quarkus Azure Functions (apps/onboarding-functions), deployed via quarkus:deploy`
- `Client framework: None (no UI). Callers use Quarkus REST clients (onboarding-ms OrchestrationApi/NotificationApi) and APIM backends`
- `API style and integration model: Synchronous HTTPS to https://selc-<env>[-pnpg]-onboarding-fn.azurewebsites.net; async via timer/queue/Durable orchestration triggers; outbound to MongoDB, Blob Storage, Key Vault, App Insights, User Registry, PEC SMTP`
- `Authentication and session model: Function key (Key Vault secret fn-onboarding-primary-key) on every HTTP call, stateless; VPN = Azure VPN Gateway point-to-site, OpenVPN, Entra ID auth, membership from vpn.members per environment; managed identities for outbound Azure resources`
- `Data model expectations: No change; function data (Onboarding, Institution, User, Notification, contract documents) untouched`
- `Deployment model: Terraform (infra/resources/onboarding-functions/<env>-<domain> using _modules/functions; VPN, DNS forwarder, private DNS zones, APIM in infra/core); app code via GitHub Actions call_release_functions.yml; rollout dev → uat → prod, ar+pnpg together`
- `Scale expectations: Unchanged; 1 worker per plan, always_on false in dev, true in uat/prod. Private access MUST NOT require a SKU change (B2 and P1v3 support private endpoints)`
- `Security expectations: No public inbound on app or SCM; inbound only from internal VNet and VPN clients; function keys still enforced; outbound IP stays NAT Gateway; GDPR personal data in transit stays on private network for inbound calls. AI/agent components: none`

## Initial Architecture (Provisional)

**Current state (from infra):** Function App has outbound VNet integration + NAT Gateway, but inbound is public with no access restrictions. Callers use the public `*.azurewebsites.net` hostname. No `privatelink.azurewebsites.net` private DNS zone exists in `infra/core` or `infra/resources`.

**Target:**

```
VPN client (vpn.members, Entra ID) ──P2S──► VPN Gateway ─┐
Container Apps (onboarding-ms/bff/cdc) ──────────────────┤   shared VNet
APIM (Internal VNet mode) ───────────────────────────────┤──► Private Endpoint (sites) ──► Function App (app + SCM)
GitHub Actions deploy job ── TO BE DECIDED ──────────────┘                                   │ public access disabled
                                                                                            └─► VNet integration ─► NAT GW ─► outbound
DNS: selc-…-onboarding-fn.azurewebsites.net ─CNAME─► privatelink.azurewebsites.net (private zone, A records for app + scm)
     linked to VNet (+ pair VNet); VPN clients resolve via existing DNS forwarder
```

1. **Inbound private access:** one private endpoint per Function App (subresource `sites`) in the shared VNet. Covers both app and SCM hostnames.
2. **Public lockdown:** public network access disabled on the Function App (app and SCM).
3. **Name resolution:** private DNS zone `privatelink.azurewebsites.net` with A records for `<app>` and `<app>.scm`, linked to the shared VNet. The public hostname is unchanged (SELC-2.4).
4. **VPN path:** existing P2S VPN Gateway + DNS forwarder in the shared VNet deliver routing and private DNS resolution to VPN clients.
5. **Outbound:** unchanged (VNet integration subnet + NAT Gateway, managed identities, Key Vault references).
6. **Auth:** function keys unchanged; the network is an added layer, not a replacement.

**Assumptions:**
- A1. Container Apps environment and APIM (Internal mode) are in, or peered with, the shared VNet and use Azure DNS so they resolve the private zone (user confirmed reachability).
- A2. The DNS forwarder forwards to Azure DNS, so VPN clients resolve zones linked to the VNet.
- A3. ar and pnpg Function Apps of one environment share the VNet, so one private DNS zone serves both (SELC-1.5).
- A4. Timer/queue/orchestration triggers depend only on outbound connectivity and are unaffected by inbound lockdown.

**Unknowns / TO BE DECIDED:**
- U1. Where the `privatelink.azurewebsites.net` zone is owned: `infra/core/_modules/dns_private` (shared) or the functions module. TO BE DECIDED.
- U2. Which subnet hosts the private endpoint (existing private-endpoint subnet or new one). TO BE DECIDED.
- U3. **Deploy path conflict:** in `call_release_functions.yml` the infra jobs already use `self-hosted` runners (Container App Jobs in the VNet, `infra/bootstrap/_modules/github_runner`), but the code-deploy jobs run `quarkus:deploy` on GitHub-hosted `ubuntu-24.04`, reaching SCM over the public internet. This conflicts with SELC-1.3 + SELC-4.2. Moving deploy jobs to the self-hosted runners: TO BE DECIDED.
- U4. Whether the pair-region VNet also needs a zone link. UNKNOWN.
- U5. How the change is ordered to keep interruptions within SELC-5.3/5.4 (create endpoint + DNS first, verify, then disable public access). TO BE DECIDED.

## Requirement Traceability

| Component / boundary | Requirements | Needs more input |
|---|---|---|
| Private endpoint + public access disabled | SELC-1.1, 1.2, 1.3, 1.4, 2.7, 2.8 | U2 |
| Private DNS zone `privatelink.azurewebsites.net` (app + scm) | SELC-2.4, 1.5, 2.1, 2.2, 2.3, 2.6 | U1, U4 |
| Shared VNet (ar + pnpg) | SELC-1.5, 2.9 | — |
| VPN Gateway P2S (Entra ID, vpn.members) + DNS forwarder | SELC-1.2, 1.3, 1.6, 2.6, 4.1 | A2 |
| Container Apps → Function App | SELC-2.1, 2.2, 2.9 | A1 |
| APIM Internal → Function App | SELC-2.3 | A1 |
| Function key auth (unchanged) | SELC-2.5 | — |
| VNet integration + NAT Gateway (unchanged) | SELC-3.1, 3.2, 3.3 | A4 |
| GitHub Actions deploy → SCM | SELC-4.2 | **U3** |
| Terraform rollout per environment | SELC-5.1–5.4 | U5 |

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR description.
- Only use libraries that are actively maintained (commit or release within the last 12 months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear security track record.
