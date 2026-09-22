# Azure Storage Identification

`onboarding-ms` can require more than one Azure Storage connection for each tenant. Storage routing is
therefore based on both the validated tenant and a stable logical storage key:

```text
JWT tenant_id + X-Tenant-Id
              ↓
        TenantContext
              +
 code-owned logical key (for example "products")
              ↓
        TenantRegistry
              ↓
 account + container + pathPrefix + authentication
              ↓
      tenant-aware AzureBlobClient
```

## 1. Why storage is a map

The `storages` property is a JSON object keyed by logical purpose, not an array:

```json
{
  "storages": {
    "products": { "...": "..." },
    "contracts": { "...": "..." }
  }
}
```

This provides deterministic lookup by `(tenantId, logicalStorageKey)`, avoids dependence on array order, and
makes duplicate keys invalid at configuration parsing time. Logical keys are application contracts and MUST
be constants or validated enum values. They never come directly from an HTTP request or blob path.

Initial `onboarding-ms` key:

- `products`: account and container containing the product catalogue.

Future keys can be added without changing the top-level schema, for example `contracts`, `attachments`,
`templates`, or `archives`.

## 2. Registry configuration

```json
{
  "AR": {
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
        "pathPrefix": "",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_AR_PRODUCTS"
        }
      },
      "contracts": {
        "account": "stselcardocuments",
        "container": "contracts",
        "pathPrefix": "onboarding",
        "authentication": {
          "type": "CONNECTION_STRING",
          "connectionStringEnvVar": "BLOB_CONNECTION_STRING_AR_CONTRACTS"
        }
      }
    }
  },
  "PNPG": {
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
        "pathPrefix": "",
        "authentication": {
          "type": "MANAGED_IDENTITY",
          "managedIdentityClientIdEnvVar": "AZURE_CLIENT_ID_PNPG_PRODUCTS"
        }
      }
    }
  }
}
```

A tenant may have many logical bindings. Different bindings may use different accounts, containers, or
authentication modes. Reusing the same physical account or container is allowed only when repeated
explicitly in the relevant bindings.

## 3. Binding fields

| Field | Required | Description |
|---|---:|---|
| `account` | yes | Azure Storage account name; used to derive the standard Blob endpoint and for diagnostics |
| `container` | yes | Container selected for the logical purpose |
| `pathPrefix` | no | Trusted prefix prepended by the provider; it is not accepted from callers |
| `authentication.type` | yes | `MANAGED_IDENTITY` or `CONNECTION_STRING` |
| `authentication.managedIdentityClientIdEnvVar` | no | Environment variable containing a user-assigned Managed Identity client ID |
| `authentication.connectionStringEnvVar` | conditional | Mandatory for `CONNECTION_STRING`; names a secret-backed environment variable |

Authentication settings are mutually exclusive:

- `MANAGED_IDENTITY` rejects `connectionStringEnvVar`;
- `CONNECTION_STRING` requires `connectionStringEnvVar` and rejects
  `managedIdentityClientIdEnvVar`.

The registry never contains a connection string, account key, SAS token, or Key Vault secret value.

## 4. Runtime resolution

For a product catalogue read under tenant `AR`:

```text
TenantContext.requiredTenantId() = AR
logicalStorageKey              = products
→ registry["AR"].storages["products"]
→ account stselcarproducts
→ container selc-d-product
→ Managed Identity configured by AZURE_CLIENT_ID_AR_PRODUCTS
→ blob products.json
```

`products.json` remains application/domain configuration; it is not part of resource selection. The registry
selects the account, container, optional trusted prefix, and authentication mechanism.

The provider caches clients by immutable account/credential identity. It resolves the binding for every
operation, so concurrent AR and PNPG requests cannot leak a previously selected client or container.

## 5. Validation and failure behavior

At startup, the registry validates every mandatory logical key for every supported tenant:

1. tenant and logical key are non-blank and normalized;
2. account and container are present;
3. authentication type is supported;
4. authentication fields are complete and mutually exclusive;
5. referenced environment variables exist and are non-blank;
6. duplicate normalized logical keys are rejected.

An unknown tenant, unknown logical key, missing environment variable, invalid binding, inaccessible account,
or missing container fails closed. The provider MUST NOT use:

- another tenant's binding;
- another logical key;
- legacy flat properties such as `onboarding-ms.blob-storage.*-product`;
- a default/shared account or container.

## 6. Infrastructure mapping

Terraform maps secret-backed values to the exact environment-variable names declared by each binding.
Managed Identity bindings grant the Container App identity only the required Blob data-plane permissions.
Connection-string bindings use distinct Key Vault-backed Container App secrets.

Adding a new logical storage purpose requires:

1. provisioning the account/container and permissions or secret;
2. exposing any referenced environment variables;
3. adding the binding for every tenant that must support it;
4. declaring whether the binding is mandatory at startup or optional until first use;
5. adding resolver, readiness, and tenant-isolation tests.
