# Mongo Database Identification

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
Tenant-aware ReactiveMongoClient
```

1. Tenant resolution

The tenant is obtained from:

- the JWT tenant_id claim, through JwtTenantValidationFilter ;
- the X-Tenant-Id header, through TenantResolutionFilter .

The value is normalized, validated against the tenant registry, and stored in
TenantContext:

```
TenantContext.tenantId = AR
```

In production, with:

```
tenant.enforcement.enabled=true
```

a missing or unknown tenant causes an error. The application does not
automatically fall back to AR .

2. Tenant registry lookup

```
The current configuration is:

{ "AR": {
    "mongo": {
      "account": "cosmos-ar",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    } }, "PNPG": {
    "mongo": {
      "account": "cosmos-pnpg",
      "database": "selcOnboarding",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG"
    } }
}
```

For AR , the registry resolves:

```
connectionStringEnvVar = MONGODB_CONNECTION_STRING_AR
database              = selcOnboarding
```

The value of the environment variable is read through MicroProfile Config:

```
MONGODB_CONNECTION_STRING_AR = mongodb://...
```

The account field is currently descriptive metadata. It is validated, but it is
not used to build the connection string. The actual Mongo/Cosmos account is the
one specified by the connection string.

3. Client creation

At startup, TenantMongoClientProducer:

1. reads all supported tenants from TenantRegistry ;
2. retrieves the connection string from the configured environment variable;
3. creates one ReactiveMongoClient for each tenant;
4. stores the clients in a map:

```
AR   → client created with MONGODB_CONNECTION_STRING_AR
PNPG → client created with MONGODB_CONNECTION_STRING_PNPG
```

If a connection string is missing for a configured tenant, the application fails
to start.

Do not set `@MongoEntity(clientName)`: Panache would also create a synthetic
named client and CDI would have two beans for the same name.

The producer instead replaces the **default** Panache `ReactiveMongoClient`
with an `@Alternative` proxy. `TenantMongoDatabaseResolver` supplies the
database name for the current `TenantContext`.

4. Client and database selection during a query

When Panache opens a collection it:

1. looks up the default reactive client (the tenant-aware alternative);
2. asks `TenantMongoDatabaseResolver` for the database name of the current
   `TenantContext`;
3. calls `getDatabase(name)` on the tenant-aware proxy.

The proxy:

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

5. Additional logical isolation

In addition to physical client/database routing, OnboardingRepository applies
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

Updates are constrained by both the entity identifier and the tenant:

```
tenantId = ?1 and _id = ?2
```

This ensures that tenant isolation remains enforced even when multiple tenants
share the same Mongo database.

