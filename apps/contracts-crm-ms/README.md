# Contracts CRM MS

Quarkus microservice that builds the **PSP contract delegations** for the CRM and publishes them
on the `SC-ContractsCRMDelegations` topic (Jira SELC-9603).

It receives the `SC-Delegations` notifications delivered by the SelfCare webhook (`apps/webhook`)
and handles only delegations of type `PSP_MEMBERSHIP` and `PSP_RECIPIENT`; other types are
acknowledged with `204 No Content`.

## Flow

For each PSP delegation (`from` = delegating PSP, `to` = delegated PSP):

1. reads from onboarding-ms (`GET /v1/onboarding`, status `COMPLETED`) the onboarding of `from`
   and of `to` for the delegation product, taking `institution.paymentServiceProvider.contractId`
   (the most recent onboarding with a contract id is used);
2. looks for the contract delegation by `productId` + `contract.id` (contract of `from`) in the
   `contractDelegations` collection of the `selcContractsCrm` database and creates it if missing;
3. sets `membership` (`PSP_MEMBERSHIP`) or `recipient` (`PSP_RECIPIENT`) with the `to`
   institution and contract, the delegation date and status (`ACTIVE` / `DELETED`); a delegation
   older than the stored one is ignored;
4. when both `membership` and `recipient` are set, sends the contract delegation to the
   `SC-ContractsCRMDelegations` Event Hub topic and forwards it to the webhook
   (`POST /webhooks/notify`, same topic, tenant `AR`).

Stored document:

```json
{
  "_id": "uuid",
  "productId": "prod-pagopa",
  "contract":   { "institutionId": "...", "id": "...", "date": "...", "status": "ACTIVE" },
  "membership": { "institutionId": "...", "id": "...", "date": "...", "status": "ACTIVE" },
  "recipient":  { "institutionId": "...", "id": "...", "date": "...", "status": "DELETED" },
  "createdAt": "...",
  "updatedAt": "..."
}
```

Errors are returned as `application/problem+json` so that the webhook can retry them:
`404` when an onboarding with PSP contract is not found, `500` when the Event Hub publication
fails. Failures of the webhook forwarding are only logged.

## Endpoints

| Method | Path                              | Description                                   |
|--------|-----------------------------------|-----------------------------------------------|
| `POST` | `/v1/delegations/notify`          | Receives an `SC-Delegations` webhook delivery |
| `GET`  | `/q/health/{live,ready,started}`  | Health probes                                 |

The endpoint requires the JWT signed by the webhook (issuer `PAGOPA`, audience
`selfcare-webhook`). Register the webhook subscription for the PSP product (e.g. `prod-pagopa`)
with `POST /webhooks` on webhook-ms:

```json
{
  "tenantId": "AR",
  "productId": "prod-pagopa",
  "url": "https://selc-<env_short>-contracts-crm-ms-ca.<container-app-environment-domain>/v1/delegations/notify",
  "httpMethod": "POST",
  "topics": ["SC-Delegations"]
}
```

## Configuration

| Variable                                                  | Description                                                  |
|-----------------------------------------------------------|--------------------------------------------------------------|
| `MONGODB_CONNECTION_STRING`, `MONGODB_DATABASE_NAME`      | MongoDB connection (database `selcContractsCrm`)              |
| `JWT_PUBLIC_KEY`                                          | Public key used to verify the webhook JWT                    |
| `JWT_TOKEN_PRIVATE_KEY`, `JWT_TOKEN_KID`                  | PKCS8 key and kid of the machine JWT sent to onboarding-ms   |
| `ONBOARDING_URL`, `WEBHOOK_URL`                           | onboarding-ms and webhook-ms base URLs                       |
| `EVENT_HUB_BASE_PATH`, `EVENT_HUB_SC_CONTRACTS_CRM_DELEGATIONS_TOPIC` | Event Hub namespace and topic                    |
| `SHARED_ACCESS_KEY_NAME`, `EVENTHUB_SC_CONTRACTSCRMDELEGATIONS_SELFCARE_WO_KEY_LC` | SAS key used to publish            |
| `EVENTHUB_SENDER_MANAGED_IDENTITY_CLIENT_ID`              | Optional: publish with a managed identity instead of SAS     |
| `CONTRACTS_CRM_WEBHOOK_ENABLED`                           | Forward the contract delegations to the webhook (default `true`) |

## Deployment

Azure Container App in the AR Container App Environment, see
`infra/resources/contracts-crm-ms/<env>-ar`. It is not exposed through APIM: it is called by the
webhook from inside the virtual network. The Event Hub topic is defined in
`infra/core/<env>-ar/locals.tf`.

## Build & Run

```bash
# Build and test
mvn --projects :contracts-crm-ms --also-make clean package

# Dev mode
mvn -f apps/contracts-crm-ms/pom.xml quarkus:dev
```
