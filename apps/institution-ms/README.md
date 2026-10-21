# selfcare-institition-ms

## Description
This Spring Boot-based microservice is designed to handle several key functionalities in the selfcare operations domain. It includes business logic for:

- Onboarding operations.
- Management of institutions and delegations.
- User creation.
- Associating users with products and institutions.

## Prerequisites
Before running the microservice, ensure you have installed:

- Java JDK 17 or higher
- Maven 3.6 or higher
- Connection to VPN selc-d-vnet

## Configuration
Look at app/src/main/resources/`application.yml` file to set up environment-specific settings, such as database details.

### Tenant-aware MongoDB

MongoDB routing uses the shared Spring tenant registry. The service requires
`TENANT_MONGODB_ENABLED=true` (the default); setting it to `false` does not
restore the legacy MongoDB client configuration.

For the existing single-tenant setup, `TENANT_SUPPORTED_TENANTS` and
`TENANT_DEFAULT` default to `AR`. The default registry keeps the existing
institution database and connection settings:

```json
{
  "AR": {
    "mongo": {
      "account": "local",
      "database": "selcMsCore",
      "connectionStringEnvVar": "INSTITUTION_MONGODB_CONNECTION"
    },
    "jwt": {
      "publicKeyEnvVar": "INSTITUTION_JWT_PUBLIC_KEY"
    },
    "storages": {}
  }
}
```

The aliases `INSTITUTION_MONGODB_CONNECTION` and
`INSTITUTION_JWT_PUBLIC_KEY` resolve to the existing MongoDB URI and
`jwt.signingKey` configuration. Continue to use `MONGODB_CONNECTION_URI`,
`MONGODB_APPNAME`, `MONGODB_NAME_SELC_MSCORE`, and `JWT_TOKEN_PUBLIC_KEY` for
this setup.

For routed connections, `tenant.mongodb.application-name` is set from
`MONGODB_APPNAME` (default `selcMsCore`). The shared tenant Mongo factory applies
this as the Mongo client application name after parsing each tenant's connection
string, so URI options are preserved without changing the Key Vault value.

To enable multiple tenants, provide `SELFCARE_TENANT_DATA_ISOLATION` as a JSON
object keyed by tenant ID and set `TENANT_SUPPORTED_TENANTS` to the explicit
comma-separated allowlist. For example:

```json
{
  "AR": {
    "mongo": {
      "account": "institution-ar",
      "database": "institution-ar",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_AR"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR"
    },
    "storages": {}
  },
  "PNPG": {
    "mongo": {
      "account": "institution-pnpg",
      "database": "institution-pnpg",
      "connectionStringEnvVar": "MONGODB_CONNECTION_STRING_PNPG"
    },
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_PNPG"
    },
    "storages": {}
  }
}
```

Set `MONGODB_CONNECTION_STRING_AR`, `MONGODB_CONNECTION_STRING_PNPG`,
`JWT_PUBLIC_KEY_AR`, and `JWT_PUBLIC_KEY_PNPG` outside the registry. Registry
fields contain environment-variable names, not credentials or key material.
Every supported tenant must have its own resolvable MongoDB binding and JWT
verification key before startup. Authenticated requests must include
`X-Tenant-Id` consistent with the verified JWT and supported-tenant allowlist.

`TENANT_STRICT_DATA_ISOLATION` defaults to `true`. In strict mode, untagged
legacy documents are not visible. Explicitly setting it to `false` allows
untagged documents only within the selected tenant database; documents marked
for another tenant remain excluded. Use that temporary mode only when the
selected database has one known legacy owner, and backfill tenant ownership
before enabling strict isolation. No automatic legacy backfill is performed.
New writes are always tagged.

The readiness group is available at `/actuator/health/readiness`. Its
`mongoReadiness` component (`mongodb-institution`) pings every supported tenant
database and is UP only when all probes succeed; the file and product blob
storage components remain included. MongoDB diagnostics retain the component,
latency, and sanitized tenant failure details, but omit the obsolete
single-database `host` and `database` fields. Tenant registry or connection changes
require an application restart because routed Mongo clients are cached.

### Tenant rollout and rollback

The existing Terraform roots at
`infra/resources/institution-ms/{dev,uat,prod}-{ar,pnpg}` each remain single
tenant. AR roots support/default to `AR` and use
`MONGODB_CONNECTION_STRING_AR`/`JWT_PUBLIC_KEY_AR`; PNPG roots support/default
to `PNPG` and use `MONGODB_CONNECTION_STRING_PNPG`/`JWT_PUBLIC_KEY_PNPG`.
Those environment variables reference the existing
`mongodb-connection-string` and `jwt-public-key` Key Vault secrets in each
root's current vault. The registry selects the existing Cosmos DB account and
`selcMsCore` database; these settings do not provision databases or add a
second tenant to a deployment. Mongo routing and strict isolation are enabled
in the Terraform baseline.

Before enabling another tenant, coordinate every producer and consumer of the
institution, delegation, notification, and graph-source collections so new
records carry tenant ownership. Configure a distinct database and JWT key for
each tenant; tenants must not share a production database because MongoDB
identifiers and existing indexes are database-wide.

Inventory untagged records and identify their owner before cutover. If a
backfill is needed, run a reviewed, tenant-specific operational procedure; the
service does not migrate production data automatically. The Terraform baseline
remains strict; an explicitly authorized temporary
`TENANT_STRICT_DATA_ISOLATION=false` override is appropriate only for one known
owner of the selected database. Backfill and coordinate producers before
re-enabling strict mode. Release the matching application and deployment
configuration together, then restart and verify `/actuator/health/readiness`
and tenant-scoped CRUD/join smoke tests.

For rollback, stop multi-tenant traffic before restoring the previous
single-database application and its Terraform environment names
`MONGODB_CONNECTION_URI` and `JWT_TOKEN_PUBLIC_KEY`. Do not direct the previous
application at mixed-tenant data; keep additive ownership metadata and do not
drop data or indexes.

## Installation and Local Startup
To run the microservice locally, follow these steps:

1. **Clone the Repository**

```shell script
git clone https://github.com/pagopa/selfcare-institution.git
cd apps/selfcare-institution-ms/
```

2. **Build the Project**

```shell script
mvn clean install
```

2. **Start the Application**

```shell script
mvn spring-boot:run -pl app
```

## Usage
After starting, the microservice will be available at `http://localhost:8080/`.

To use the API, refer to the Swagger UI documentation (if available) at `http://localhost:8080/swagger-ui.html`.

## Cucumber Tests (Integration Tests)
A new suite of integration tests written with cucumber was added in `it.pagopa.selfcare.mscore.integration_test` package.

To run the Cucumber tests locally, execute it.pagopa.selfcare.user.integration_test.CucumberSuite

To run a single test or a specific feature file, open the file and press the play button for the corresponding test (or the file). 

### Tenant MongoDB regression tests

`TenantDelegationAggregationIntegrationTest` uses a MongoDB Testcontainer to cover
tenant-scoped aggregations, reachable foreign graph nodes, notifications, and
institution replacement writes in strict and compatibility modes. It verifies
that rejected writes leave stored documents unchanged and do not insert retry IDs.
With Docker running, execute it from the monorepo root:

```shell script
mvn --projects :selc-ms-core-app --also-make test -Dtest=TenantDelegationAggregationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```
