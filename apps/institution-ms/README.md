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
institution database and connection settings. The MongoDB/JWT examples below
must also include the required bindings described under
[Tenant-aware blob storage](#tenant-aware-blob-storage):

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

### Tenant-aware blob storage

Blob access uses the shared Spring `TenantBlobClientProvider` and requires the
`products` and `mail-templates` storage bindings for every supported tenant.
Each binding declares an account, container, optional trusted `pathPrefix`, and
an explicit authentication mode. For Managed Identity, the registry stores
only the environment-variable name for the client ID; for connection strings,
it stores only the environment-variable name for the connection string. Values
are supplied outside the registry and are validated during startup. The product
catalog path remains the domain file `products.json`; the delegation email
template path remains `MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH`.

A single-tenant deployment sets `TENANT_SUPPORTED_TENANTS=AR` and includes both
bindings under the existing `AR` registry entry:

```json
{
  "AR": {
    "storages": {
      "products": {
        "account": "product-storage",
        "container": "selc-d-product",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "INSTITUTION_PRODUCTS_STORAGE_CLIENT_ID"
        }
      },
      "mail-templates": {
        "account": "documents-storage",
        "container": "sc-d-documents-blob",
        "pathPrefix": "templates",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "INSTITUTION_MAIL_TEMPLATES_STORAGE_CLIENT_ID"
        }
      }
    }
  }
}
```

The `storages` object is merged into (not substituted for) the tenant's existing
`mongo` and `jwt` registry entries. To run one application with two tenants,
set `TENANT_SUPPORTED_TENANTS=AR,PNPG` and repeat both bindings under each
tenant, pointing each purpose to that tenant's existing account, container,
path, and Managed Identity. Use distinct client-ID environment-variable
references per tenant and purpose. `TENANT_STORAGE_MANDATORY_KEYS` defaults to
`products,mail-templates`; missing bindings or referenced credentials prevent
startup. Both readiness indicators probe every supported tenant without an
HTTP tenant context, and any failed probe marks readiness DOWN.

Apply registry changes with the matching application release and restart the
application; do not rely on runtime fallback to the previous global storage
variables. For rollback, restore the previous application and its matching
single-tenant registry/configuration together. No storage resources or blob
data are provisioned, migrated, or consolidated by this change.

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
second tenant to a deployment. Mongo routing is enabled in the Terraform
baseline, while `TENANT_STRICT_DATA_ISOLATION=false` preserves access to legacy
untagged documents within each deployment's single-tenant database.

Before enabling another tenant, coordinate every producer and consumer of the
institution, delegation, notification, and graph-source collections so new
records carry tenant ownership. Configure a distinct database and JWT key for
each tenant; tenants must not share a production database because MongoDB
identifiers and existing indexes are database-wide.

Inventory untagged records and identify their owner before cutover. If a
backfill is needed, run a reviewed, tenant-specific operational procedure; the
service does not migrate production data automatically. Keep the compatibility
setting only while there is one known owner of the selected database. Backfill
and coordinate producers before setting `TENANT_STRICT_DATA_ISOLATION=true`.
Release the matching application and deployment
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
