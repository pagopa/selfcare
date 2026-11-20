# Webhook Consumer

Quarkus microservice used as a **test target for the SelfCare webhook** (`apps/webhook`).
It receives the notifications delivered by the webhook and logs, for each one:

- the HTTP method;
- the `Authorization` header (other headers are not logged);
- the full request body (the notification payload).

Line breaks in logged values are escaped to prevent log forging. Every request is acknowledged
with `204 No Content`.

## Endpoints

| Method                 | Path                              | Description                     |
|------------------------|-----------------------------------|---------------------------------|
| `POST`, `PUT`, `PATCH` | `/notifications`                  | Receives and logs notifications |
| `GET`                  | `/q/health/{live,ready,started}`  | Health probes                   |

Register a webhook with URL
`https://selc-<env_short>-webhook-consumer-ca.<container-app-environment-domain>/notifications`
and one of the HTTP methods above.

## Deployment

Deployed as an Azure Container App in the SelfCare Container App Environment (AR domain), see
`infra/resources/webhook-consumer/<env>-ar`. It runs with **at most one replica** in every
environment and is **not exposed through APIM / Application Gateway**: it is reachable only from
inside the virtual network (VPN).

## Build & Run

```bash
# Build and test
mvn --projects :webhook-consumer --also-make clean package

# Dev mode
mvn -f apps/webhook-consumer/pom.xml quarkus:dev

# Try it
curl -X POST http://localhost:8080/notifications \
  -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
  -d '{"tenantId":"AR","productId":"prod-io","topic":"SC-Contracts","payload":{}}'
```
