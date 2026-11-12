# selfcare-sdk-tenant-mongodb

> Quarkus MongoDB tenant routing: one reactive Mongo client per supported tenant and Panache database resolution based on the current `TenantContext`.

| | |
|---|---|
| Path | `libs/selfcare-sdk-tenant-mongodb` |
| Artifact | `it.pagopa.selfcare:selfcare-sdk-tenant-mongodb:0.4.2` |
| Stack | Quarkus (`quarkus-mongodb-client`, `quarkus-mongodb-panache-common`, BOM 3.11.2), Java 17, reactive (Mutiny); parent [`selfcare-sdk-pom`](selfcare-sdk-pom.md) |
| Used by | `auth`, `document-ms`, `iam`, `onboarding-ms`, `product`, `product-cdc` |

## What it provides

Package `it.pagopa.selfcare.tenant.mongodb`:

| Class | Purpose |
|---|---|
| `TenantMongoClientProducer` | `@Singleton` that, at startup, creates a `ReactiveMongoClient` for every tenant in `TenantRegistry.supportedTenantIds()` using the tenant connection string (fails fast if one is missing). It `@Produces @Alternative @Priority(1)` a tenant-aware `ReactiveMongoClient` that replaces the Panache default one, and exposes `clientForTenant(tenantId)`, `databaseForCurrentTenant()`, `collection(name, type)`. Codec registry mirrors Quarkus (POJO automatic mapping, BSON discriminators, custom codec providers). |
| `TenantMongoDatabaseResolver` | Panache `MongoDatabaseResolver`: returns `mongo.database` of the tenant in `TenantContext` when `@MongoEntity.database` is empty. |

Source: `libs/selfcare-sdk-tenant-mongodb/src/main/java/it/pagopa/selfcare/tenant/mongodb/`.
Note from the class Javadoc: do not set `@MongoEntity(clientName)`, otherwise Panache creates a synthetic named
client and CDI injection becomes ambiguous.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-sdk-tenant-mongodb</artifactId>
  <version>0.4.2</version>
</dependency>
```

- Depends on [`selfcare-sdk-tenant`](selfcare-sdk-tenant.md): configure `tenant.registry.json` with a `mongo`
  section (`account`, `database`, `connectionStringEnvVar`) per tenant and `tenant.supported-tenants`.
- The current tenant must be in `TenantContext` before any repository call (e.g. via `TenantResolutionFilter`
  of [`selfcare-sdk-security`](selfcare-sdk-security.md)); otherwise `UnresolvedTenantException` is thrown.
- Leave `database` empty in `@MongoEntity` so that the resolver picks the tenant database.

## Build and release

```bash
mvn -f libs/selfcare-sdk-tenant-mongodb/pom.xml clean verify
```

Released by `.github/workflows/release-sdk.yml` / `.github/workflows/release_sdk_pom.yml` (matrix module
`selfcare-sdk-tenant-mongodb`, push on `main` or `workflow_dispatch`) to GitHub Packages
`https://maven.pkg.github.com/pagopa/selfcare`.

## References

- [selfcare-sdk-tenant](selfcare-sdk-tenant.md)
- [Multitenancy](../multitenancy.md)
