# selfcare-external-api-backend

Selfcare External API service, integrating with product-ms through REST.

## Product service configuration

Set `MS_PRODUCT_URL` to the base URL of product-ms (default: `http://localhost:8080`). The generated Feign client calls the product endpoints with the optional `tenantId` query parameter omitted.

## Tenant isolation (Multitenant Step 2)

The application reuses `selc-commons-tenant`, `selc-commons-web` and
`selc-commons-connector-rest` 2.23.0, as in registry-proxy. It is stateless:
`tenant.mongodb.required=false`; Mongo queries and migration flags belong to the
downstream services, not this application.

Every protected request must carry one `X-Tenant-Id` header and a signed JWT with
the same `tenant_id`. Verification uses that tenant's RSA key, issuer/audience
allowlists, RS256, expiration and not-before. Missing, duplicated, unknown or
mismatched tenants are rejected before business logic. Both request contexts are
cleared after success and failure.

Internal Feign clients propagate the authenticated bearer and the validated
`TenantContext`; they never derive the tenant from raw request headers. Product
calls continue to omit the optional `tenantId` query parameter, so product-ms
resolves the propagated tenant itself.

User Registry uses only the API key selected from the current tenant's top-level
`userRegistry` binding. Internal bearer and tenant headers are not sent to this
external provider. Keys are mandatory for all supported tenants and validated
when the client is initialized; the former global API-key setting is not a
fallback. `USERVICE_USER_REGISTRY_URL` remains the explicitly shared provider
endpoint within each deployment; its existing environment-specific value is
unchanged, including the UAT PDV endpoint currently configured in PROD PNPG.

### Environment configuration

Each `infra/resources/external-api/{dev,uat,prod}-{ar,pnpg}/locals.tf` owns its
complete `SELFCARE_TENANT_DATA_ISOLATION` JSON, supported/default tenant and
`JWT_TENANT_CLAIM_REQUIRED=true`. No external-api resource registry is defined in
the shared `local-env` module. Secret values remain in Key Vault; the JSON contains
only the names of secret-backed environment variables.

For example, the AR development registry has this shape:

```json
{
  "AR": {
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR",
      "issuers": ["SPID", "PAGOPA"],
      "audiences": ["api.dev.selfcare.pagopa.it"]
    },
    "userRegistry": {
      "apiKeyEnvVar": "USER_REGISTRY_API_KEY_AR"
    },
    "storages": {
      "contracts": {
        "account": "scdweuardocumentsst01",
        "container": "sc-d-documents-blob",
        "authentication": {
          "type": "CONNECTION_STRING",
          "connectionStringEnvVar": "CONTRACTS_STORAGE_CONNECTION_STRING_AR"
        }
      }
    }
  }
}
```

The retained `AzureStorage` connector resolves the code-owned `contracts` binding
through `TenantBlobClientProvider` for every operation. Missing context or binding
fails closed; no legacy connection string, container or other tenant is a
fallback. Blob names cannot escape the binding's configured prefix.

The six stacks retain the existing accounts, containers and Key Vault secrets:
AR uses the documents account, while PNPG retains the account referenced by
`web-storage-connection-string`. This does not provision or migrate containers.
The connector has no current business caller: contract APIs use document-ms.
Confirm the PNPG contract container and its grants before enabling direct Blob
downloads; do not infer availability from the configured binding. Unused product
Blob credentials/settings are removed; product data is obtained through REST.

### Rollout prerequisites

The Dockerfile copies `libs/` and builds commons 2.23.0 from source through
`--also-make`; publishing those artifacts is only required for builds outside the
reactor. The Spring `selc-starter-parent` remains external and is resolved from the
Azure Artifacts `selfcare-platform` feed, which must remain available until that
parent is migrated.

Deploy the new image and its renamed secret-backed variables together in one
Container App revision; the new environment names are not compatible with an old
image. Local runs must also provide the registry, its referenced credentials and
the supported tenant.

The existing stacks remain single-tenant. A shared deployment requires both
tenant definitions, distinct credential references, mandatory JWT tenant claims
and tenant-aware/reachable downstream services. The application rejects a shared
configuration that disables the claim requirement. Confirm APIM subscription
bindings, provider-side User Registry isolation, Blob ownership/access and
downstream migration/strict-mode readiness before consolidating traffic.
Infrastructure validation alone does not establish those runtime guarantees.
Keep the legacy stacks and a tested rollback route until the environment checks
pass. This change does not perform infrastructure apply, backfill or cutover.
