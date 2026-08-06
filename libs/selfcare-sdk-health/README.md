# selfcare-sdk-health

Cross-cutting SmallRye Health / MicroProfile Health helpers for **Selfcare** Quarkus microservices.

## Why

Quarkus applications get a `/q/health/ready` endpoint out of the box, but by default it only
reflects the health checks auto-registered by the extensions on the classpath (e.g.
`quarkus-mongodb-client` registers a `listDatabaseNames` check). It does not verify that the
application can actually **reach and use** its downstream dependencies — a Managed Identity may
obtain a valid token, a container may resolve DNS, an HTTP client may open a connection, and
yet the actual read/write operation may still fail (403, permission mismatch, wrong storage
account, unreachable route, and so on).

This SDK provides small reactive base classes so every Selfcare Quarkus microservice can add
readiness checks that actually exercise the downstream dependency, so that a broken dependency
translates into `/q/health/ready` returning `DOWN` and the container is taken out of the load
balancer.

## What's inside

Package `it.pagopa.selfcare.commons.health`:

| Class | Purpose |
|---|---|
| `AbstractAsyncReadinessCheck` | Implements `AsyncHealthCheck` with per-check timeout, error data enrichment, and best-effort latency reporting. Subclasses provide `checkName()` + `probe()`. |
| `AbstractBlobStorageReadinessCheck` | Convenience base for Azure Blob Storage. Auto-fills `data` with `account` / `container` and (optionally) `probeTarget` — a free-form marker of what the probe is actually calling (a specific blob path such as `products.json`, an unlikely-to-exist prefix such as `__healthcheck_probe__/` used for list-based probes, ...). |
| `AbstractMongoReadinessCheck` | Convenience base for MongoDB `ping`-style checks. Auto-fills `data` with `database` and (optionally, since 0.2.0) `host` when the subclass overrides `host()`. Ships a `hostFromConnectionString(...)` static utility to parse standard `mongodb://` / `mongodb+srv://` URIs and strip credentials. |
| `HealthCheckConstants` | Stable data-key names + default timeout (2s). |

## Getting started

### 1. Add the dependency

```xml
<dependency>
    <groupId>it.pagopa.selfcare</groupId>
    <artifactId>selfcare-sdk-health</artifactId>
    <version>0.2.0</version>
</dependency>
```

You also need `quarkus-smallrye-health` in the consuming app so that `/q/health/*` endpoints are
exposed:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

### 2. Recommended `application.properties`

```
quarkus.smallrye-health.root-path=/q/health
```
(This is the Quarkus default — declare it explicitly for clarity and consistency across services.)

### 3. Write a check

```java
@Readiness
@ApplicationScoped
public class ProductBlobReadinessCheck extends AbstractBlobStorageReadinessCheck {

    @Inject AzureBlobClient client;

    @ConfigProperty(name = "onboarding-ms.blob-storage.account-name-product") String account;
    @ConfigProperty(name = "onboarding-ms.blob-storage.container-product")    String container;

    @Override protected String checkName()   { return "blob-storage-product"; }
    @Override protected String account()     { return account; }
    @Override protected String container()   { return container; }
    @Override protected String probeTarget() { return "products.json"; }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> client.getProperties(container(), probeTarget()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
```

The health check is automatically picked up by SmallRye Health and appears under
`GET /q/health/ready`.

### 4. Response schema

Successful response (UP):
```json
{
  "name": "blob-storage-product",
  "status": "UP",
  "data": {
    "component": "blob-storage",
    "account":   "selcpweupnpgcheckoutst01",
    "container": "product",
    "probeTarget": "products.json",
    "latencyMs": "23"
  }
}
```

Failure response (DOWN):
```json
{
  "name": "blob-storage-product",
  "status": "DOWN",
  "data": {
    "component": "blob-storage",
    "account":   "selcpweupnpgcheckoutst01",
    "container": "product",
    "probeTarget": "products.json",
    "latencyMs": "134",
    "error":     "BlobStorageException: Status code 403, ..."
  }
}
```

The stable `data.error` field makes it trivial to alert on specific failure modes (e.g. any
readiness check whose `error` starts with `BlobStorageException`).

## Threading

The `probe()` `Uni` is subscribed on the same thread that invokes `/q/health/ready`, which under
Quarkus is a Vert.x event-loop thread. **Do not block it.** Wrap blocking clients with
`Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` (or
equivalent) in your concrete check.

## Timeouts

Default timeout is 2 seconds. Override `timeout()` to tune it per check. The timeout is enforced
by the SDK regardless of the client's own timeout, so a misconfigured HTTP client cannot hang the
readiness endpoint.
