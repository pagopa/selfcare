# Mongo Database Identification

This document defines the common tenant-aware Mongo routing contract for Quarkus
and Spring services. The resource-selection rules are shared; the framework
integration is implementation-specific.

The correct Mongo database is identified through the following chain:

```text
JWT/X-Tenant-Id header
        ↓
TenantContext
        ↓
TenantRegistry
        ↓
connectionStringEnvVar + database
        ↓
Tenant-aware Mongo client/factory
```

## 1. Tenant resolution

The tenant is obtained from the validated security boundary:

- the reconciled JWT `tenant_id` claim and `X-Tenant-Id` header;
- the authenticated `TenantContext` populated by the framework security integration.

The value is normalized, validated against the tenant registry, and stored in
TenantContext:

```
TenantContext.tenantId = AR
```

A missing, unknown, inconsistent, or unmapped tenant causes an error. Resource
selection MUST NOT fall back to AR, PNPG, a legacy client, or a generic default.

## 2. Tenant registry lookup

```
The current configuration is:

{ "AR": {
    "mongo": {
      "account": "cosmos-ar",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR"
    },
    "storages": {
      "products": {
        "account": "stselcarproducts",
        "container": "selc-d-product",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_AR_PRODUCTS"
        }
      }
    }
  }, "PNPG": {
    "mongo": {
      "account": "cosmos-pnpg",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG"
    },
    "storages": {
      "products": {
        "account": "stpnpgproducts",
        "container": "selc-d-product",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_PNPG_PRODUCTS"
        }
      }
    }
  }
}
```

The `storages` dimension is independent from Mongo routing and is described in
`Storage_identification.md`. It is included here to show the canonical registry shape: each tenant has one
Mongo definition and can have multiple logical storage bindings.

For AR , the registry resolves:

```
connectionStringEnvVar = MONGODB_CONNECTION_STRING_AR
database              = selcOnboarding
```

The value of the environment variable is resolved by the application's
configuration system:

```
MONGODB_CONNECTION_STRING_AR = mongodb://...
```

The account field is currently descriptive metadata. It is validated, but it is
not used to build the connection string. The actual Mongo/Cosmos account is the
one specified by the connection string.

## 3. Client creation

At startup, the service-specific Mongo configuration:

1. reads all supported tenants from TenantRegistry ;
2. retrieves the connection string from the configured environment variable;
3. creates one Mongo client/factory for each tenant;
4. stores the clients in a map:

```
AR   → client created with MONGODB_CONNECTION_STRING_AR
PNPG → client created with MONGODB_CONNECTION_STRING_PNPG
```

If a connection string is missing for a configured tenant, the application fails
to start.

For Quarkus services, do not set `@MongoEntity(clientName)`: Panache would also
create a synthetic named client and CDI would have two beans for the same name.

Quarkus services may expose the tenant-aware client through the default Panache
client/factory. Spring services MUST replace the default Mongo auto-configuration
with a tenant-aware `MongoDatabaseFactory`/`MongoTemplate` integration so that
repository code cannot select a tenant database directly.

### Spring implementation (`user-group-ms`)

`user-group-ms` imports the shared configurations from `selc-commons-tenant` and
excludes `MongoAutoConfiguration`. `TenantMongoDatabaseFactory` creates one
`MongoClient` and one `SimpleMongoClientDatabaseFactory` per supported tenant.
The primary `MongoTemplate` delegates to that factory.

The factory ignores the database name supplied by a caller and always selects the
database declared in the current tenant's registry entry. A request without a
resolved `TenantContext` therefore cannot access Mongo data. Clients are closed
when the application context is destroyed.

## 4. Client and database selection during a query

When a repository opens a collection it:

1. looks up the tenant-aware Mongo client/factory;
2. reads the current tenant from `TenantContext`;
3. selects the client's configured database for that tenant.

The tenant-aware factory:

1. reads the tenant from TenantContext ;
2. retrieves the corresponding definition from TenantRegistry ;
3. selects the correct Mongo client;
4. opens the database name provided by the resolver (or the registry if none
   was passed).

Example for AR:

```
TenantContext = AR
→ client MONGODB_CONNECTION_STRING_AR
→ database selcOnboarding
```

Example for PNPG:

```
TenantContext = PNPG
→ client MONGODB_CONNECTION_STRING_PNPG
→ database selcOnboarding
```

Therefore, even if the database name is the same, the two tenants can point to
different Cosmos/Mongo accounts.

## 5. Additional logical isolation

In addition to physical client/database routing, tenant-owned repositories apply
the tenantId discriminator to Mongo queries:

```
{ "$and": [
    {
      "tenantId": "AR"
    },
    {
      "status": "COMPLETED"
    } ]
}
```

In Spring Data repositories and services, updates are constrained by both the
entity identifier and the tenant:

```
tenantId = ?1 and _id = ?2
```

Writes and upserts explicitly set `tenantId`; upserts also set it through
`$setOnInsert`, because a top-level tenant query does not populate the inserted
document. Migration mode may temporarily include `tenantId IS NULL` in reads,
controlled by `tenant.strict-data-isolation`; strict mode removes that
compatibility path.

This ensures that tenant isolation remains enforced even when multiple tenants
share the same Mongo database. Tenant-qualified unique indexes remain an
infrastructure and migration requirement.
