# FN_VPN — Onboarding Function App on private network

Scope: Azure Function Apps created from `infra/resources/_modules/functions` and deployed through `infra/resources/onboarding-functions/<env>-<domain>` (`dev|uat|prod` × `ar|pnpg`). Today the Function App is reachable from the public internet (`https://selc-<env>[-pnpg]-onboarding-fn.azurewebsites.net`). Target: reachable only from the internal network protected by the VPN.

## Required Requirement Inputs

- `Project purpose: Selfcare (PagoPA) onboarding Function App — runs onboarding orchestrations, notifications and institution/user side-effects for Italian public-sector and private entities (SELC "ar" and PNPG domains). Change goal: remove public internet exposure.`
- `Primary users / actors: Internal callers onboarding-ms, onboarding-bff, onboarding-cdc, running on Azure Container Apps with network reachability to the Function App; APIM external API (backend "BACKEND_BASE_URL"), deployed in a VNet with network reachability to the Function App; support scripts run by operators connected to the VPN, invoking HTTP-triggered functions; operators/developers via VPN`
- `Core workflows: Onboarding orchestration start/status, notification dispatch, institution/external functions via HTTP triggers; timer/queue-driven functions; operator diagnostics via VPN`
- `Business objects / data entities: Onboarding, Institution, User, Notification, contract documents (Blob Storage); data model unchanged by this requirement`
- `External integrations: MongoDB, Azure Blob Storage, Key Vault, Application Insights, User Registry (PDV), PEC SMTP, APIM; outbound egress via NAT Gateway`
- `Authentication / roles: Function key (Key Vault secret "fn-onboarding-primary-key") used by callers; VPN access granted per environment to users listed in vpn.members of the Terraform AD/users config (prefix selc-d/selc-u/selc-p); Azure AD groups (<prefix>-adgroup-admin|developers|security|technical-project-managers|operations|externals) grant Azure roles, not VPN access`
- `Regulatory or privacy constraints: Processes personal data of institution representatives (GDPR); specific constraints: TO BE DECIDED`

## Functional Requirements

### SELC-1 Network exposure
- **SELC-1.1** The Function App HTTP endpoints MUST NOT be reachable from the public internet; a request from a public IP outside the VPN MUST be rejected or fail to connect.
- **SELC-1.2** The Function App HTTP endpoints MUST be reachable from clients connected to the VPN.
- **SELC-1.3** The Function App SCM/Kudu endpoint MUST NOT be reachable from the public internet and MUST be reachable from clients connected to the VPN.
- **SELC-1.4** The requirement MUST apply to every environment/domain deployed from the shared module (dev/uat/prod, ar/pnpg).
- **SELC-1.5** PNPG and SELC "ar" Function Apps share the same VNet/VPN; a client connected to that VPN MUST be able to reach both Function Apps of the same environment.
- **SELC-1.6** Only users listed in `vpn.members` of the environment configuration MUST be able to connect to the VPN and reach the Function App of that environment; users not listed MUST NOT.

### SELC-2 Internal callers
- **SELC-2.1** onboarding-ms MUST keep invoking Orchestration and Notification APIs of the Function App successfully after the change.
- **SELC-2.2** onboarding-bff and onboarding-cdc MUST keep invoking the Function App successfully after the change.
- **SELC-2.3** APIM external API operations backed by the Function App MUST keep returning the same responses as before the change, calling the Function App over the VNet it is deployed in.
- **SELC-2.4** The existing Function App hostname (`selc-<env>[-pnpg]-onboarding-fn.azurewebsites.net`) MUST be kept; from inside the internal network and the VPN it MUST resolve to a private address (private DNS override), so callers MUST NOT need to change their configured URL.
- **SELC-2.5** Existing function-key authentication MUST remain enforced; network restriction MUST NOT replace it.
- **SELC-2.6** Support scripts run from a host connected to the VPN MUST be able to invoke HTTP-triggered functions and MUST receive the same responses as before the change.
- **SELC-2.7** Support scripts run from a host not connected to the VPN MUST NOT be able to reach the Function App.
- **SELC-2.8** No caller other than internal services, APIM and support scripts over the VPN MUST be able to invoke the Function App.
- **SELC-2.9** Calls from SELC-2.1 and SELC-2.2 MUST keep originating from Azure Container Apps over the internal network, without traversing the public internet.

### SELC-3 Function behavior
- **SELC-3.1** Timer-, queue- and orchestration-triggered functions MUST keep executing with the same outcomes after the change.
- **SELC-3.2** Outbound calls (MongoDB, Blob Storage, Key Vault, User Registry, SMTP, Application Insights) MUST keep succeeding; outbound public IP MUST remain the NAT Gateway IP where external allow-lists depend on it.
- **SELC-3.3** Key Vault references in app settings MUST keep resolving.

### SELC-4 Operations
- **SELC-4.1** Operators connected to the VPN SHOULD be able to invoke health/diagnostic endpoints and view logs.
- **SELC-4.2** GitHub Actions deployment of the Function App code MUST keep succeeding after public access is disabled, using runners that reach the private network.

### SELC-5 Rollout
- **SELC-5.1** The change MUST be rolled out in order dev → uat → prod, applying ar and pnpg together in each environment.
- **SELC-5.2** An environment MUST NOT be started until all callers (SELC-2) are verified in the previous one.
- **SELC-5.3** In dev and uat a short service interruption MAY occur without a maintenance window.
- **SELC-5.4** In prod any service interruption MUST be short and occur only within an agreed maintenance window.

## Open Questions

None.
