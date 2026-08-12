# apim_api

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api"></a> [apim\_api](#module\_apim\_api) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v9.4.0 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_api_management_api_version_set.apim_api_version_set](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_api_dns_zone_prefix"></a> [api\_dns\_zone\_prefix](#input\_api\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"api.selfcare"` | no |
| <a name="input_api_name"></a> [api\_name](#input\_api\_name) | The name of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_api_operation_policies"></a> [api\_operation\_policies](#input\_api\_operation\_policies) | List of api policy for given operation. | <pre>list(object({<br/>    operation_id = string<br/>    xml_content  = string<br/>    }<br/>  ))</pre> | `[]` | no |
| <a name="input_apim_name"></a> [apim\_name](#input\_apim\_name) | The name of the API Management instance. | `string` | n/a | yes |
| <a name="input_apim_rg"></a> [apim\_rg](#input\_apim\_rg) | The name of the resource group in which the API Management instance exists. | `string` | n/a | yes |
| <a name="input_base_path"></a> [base\_path](#input\_base\_path) | The base path of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_default_tenant_id"></a> [default\_tenant\_id](#input\_default\_tenant\_id) | Tenant assigned to requests that carry neither an Origin nor a Referer header AND whose subscription is not listed in service\_caller\_tenants. Defaults to null, meaning such requests are REJECTED with 403 (fail-closed). Prefer service\_caller\_tenants for known server-to-server callers. Only set this on APIs that provably receive non-browser traffic that cannot be enumerated, and keep it reviewable: a blanket default would let any non-browser caller pick a tenant simply by omitting Origin, which is exactly the silent fallback the multitenant DoD forbids (apps/docs/Multitenant/Step\_0/EPIC.md sub-task 2). | `string` | `null` | no |
| <a name="input_default_tenant_operation_ids"></a> [default\_tenant\_operation\_ids](#input\_default\_tenant\_operation\_ids) | Operation ids allowed to use default\_tenant\_id even when Origin/Referer is present but not a tenant frontend (for example a SAML ACS receiving a browser POST from the IdP origin). Empty by default. Keep this list minimal: the fallback is scoped to the named operation and never applies API-wide to an unknown origin. | `set(string)` | `[]` | no |
| <a name="input_display_name"></a> [display\_name](#input\_display\_name) | The display name of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_local_development_origins"></a> [local\_development\_origins](#input\_local\_development\_origins) | Extra origins allowed for local frontend development (e.g. http://localhost:3000). These are added to the CORS allow-list AND resolve to tenant\_ids[0]. CORS here runs with allow-credentials=true, so a localhost origin lets any process listening on that port on a user's machine issue credentialed calls to this API: it MUST stay empty outside dev. Populated centrally from module.local.config.local\_development\_origins, which is non-empty only when env is dev. | `list(string)` | `[]` | no |
| <a name="input_openapi_path"></a> [openapi\_path](#input\_openapi\_path) | Path to the OpenAPI specification file. | `string` | n/a | yes |
| <a name="input_private_dns_name"></a> [private\_dns\_name](#input\_private\_dns\_name) | The private DNS name of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_service_caller_tenants"></a> [service\_caller\_tenants](#input\_service\_caller\_tenants) | Maps an APIM subscription id (map key) to the tenant assigned to it (map value, AR or PNPG). Applies only to server-to-server callers, i.e. requests carrying neither Origin nor Referer, and is evaluated BEFORE default\_tenant\_id. This is the only supported way for a backend service calling this API through APIM to be attributed to a tenant: the inbound policy overrides X-Tenant-Id unconditionally, so a header set by the calling application is discarded. Prefer one subscription per (calling service, tenant) pair over widening default\_tenant\_id, so that each s2s caller's tenant is pinned to a credential it cannot change. | `map(string)` | `{}` | no |
| <a name="input_tenant_ids"></a> [tenant\_ids](#input\_tenant\_ids) | Tenants served by this API group (multitenant migration, see apps/docs/Multitenant/Step\_0/REQUIREMENTS.md SELC-1/SELC-2). Each entry maps a tenant id (AR/PNPG) to its frontend origin (scheme + host, e.g. https://selfcare.pagopa.it). APIM allow-lists every listed origin for CORS and resolves X-Tenant-Id at request time by matching the calling origin EXACTLY (scheme + authority, Referer reduced to the same form) against this list, always discarding any X-Tenant-Id sent by the caller. Requests whose origin is not listed are rejected with 403; requests with no Origin/Referer at all are governed by var.default\_tenant\_id, not by this list. | <pre>list(object({<br/>    id     = string<br/>    origin = string<br/>  }))</pre> | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
