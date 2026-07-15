# key_vault

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
| <a name="module_key_vault"></a> [key\_vault](#module\_key\_vault) | github.com/pagopa/terraform-azurerm-v4.git//key_vault | v9.6.1 |
| <a name="module_secrets_selfcare_status_dev"></a> [secrets\_selfcare\_status\_dev](#module\_secrets\_selfcare\_status\_dev) | github.com/pagopa/terraform-azurerm-v4.git//key_vault_secrets_query | v9.6.1 |
| <a name="module_secrets_selfcare_status_uat"></a> [secrets\_selfcare\_status\_uat](#module\_secrets\_selfcare\_status\_uat) | github.com/pagopa/terraform-azurerm-v4.git//key_vault_secrets_query | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.adgroup_admin_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.adgroup_developers_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.adgroup_externals_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.adgroup_security_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.azdevops_iac_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.azdo_sp_tls_cert](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.azure_cdn_frontdoor_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_resource_group.sec_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_key_vault_secret.adgroup_admin](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.adgroup_developers](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.adgroup_externals](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.adgroup_security](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.app_projects_principal](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.azdo_sp_tls_cert](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.iac_principal](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.vpn_app](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_subscription.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subscription) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_azdo_sp_tls_cert_enabled"></a> [azdo\_sp\_tls\_cert\_enabled](#input\_azdo\_sp\_tls\_cert\_enabled) | n/a | `string` | `false` | no |
| <a name="input_azuread_service_principal_azure_cdn_frontdoor_id"></a> [azuread\_service\_principal\_azure\_cdn\_frontdoor\_id](#input\_azuread\_service\_principal\_azure\_cdn\_frontdoor\_id) | n/a | `string` | `"f3b3f72f-4770-47a5-8c1e-aa298003be12"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | The prefix for the project, e.g., selc | `string` | `"selc"` | no |
| <a name="input_project"></a> [project](#input\_project) | The name of the project for eg. selc-d or selc-d-pnpg | `string` | n/a | yes |
| <a name="input_sku_name"></a> [sku\_name](#input\_sku\_name) | n/a | `string` | `"standard"` | no |
| <a name="input_soft_delete_retention_days"></a> [soft\_delete\_retention\_days](#input\_soft\_delete\_retention\_days) | (Optional) The number of days that items should be retained for once soft-deleted. This value can be between 7 and 90 (the default) days. | `number` | `15` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_adgroup_admin_id"></a> [adgroup\_admin\_id](#output\_adgroup\_admin\_id) | n/a |
| <a name="output_adgroup_developers_id"></a> [adgroup\_developers\_id](#output\_adgroup\_developers\_id) | n/a |
| <a name="output_adgroup_externals_id"></a> [adgroup\_externals\_id](#output\_adgroup\_externals\_id) | n/a |
| <a name="output_adgroup_security_id"></a> [adgroup\_security\_id](#output\_adgroup\_security\_id) | n/a |
| <a name="output_app_projects_principal_object_id"></a> [app\_projects\_principal\_object\_id](#output\_app\_projects\_principal\_object\_id) | n/a |
| <a name="output_iac_principal_object_id"></a> [iac\_principal\_object\_id](#output\_iac\_principal\_object\_id) | n/a |
| <a name="output_key_vault_id"></a> [key\_vault\_id](#output\_key\_vault\_id) | n/a |
| <a name="output_key_vault_name"></a> [key\_vault\_name](#output\_key\_vault\_name) | n/a |
| <a name="output_key_vault_resource_group_name"></a> [key\_vault\_resource\_group\_name](#output\_key\_vault\_resource\_group\_name) | n/a |
| <a name="output_sec_rg_location"></a> [sec\_rg\_location](#output\_sec\_rg\_location) | n/a |
| <a name="output_sec_rg_name"></a> [sec\_rg\_name](#output\_sec\_rg\_name) | n/a |
| <a name="output_secrets_selfcare_status_dev"></a> [secrets\_selfcare\_status\_dev](#output\_secrets\_selfcare\_status\_dev) | Secrets query outputs |
| <a name="output_secrets_selfcare_status_uat"></a> [secrets\_selfcare\_status\_uat](#output\_secrets\_selfcare\_status\_uat) | n/a |
| <a name="output_subscription_id"></a> [subscription\_id](#output\_subscription\_id) | n/a |
| <a name="output_subscription_name"></a> [subscription\_name](#output\_subscription\_name) | n/a |
| <a name="output_tenant_id"></a> [tenant\_id](#output\_tenant\_id) | n/a |
| <a name="output_vpn_app_client_id"></a> [vpn\_app\_client\_id](#output\_vpn\_app\_client\_id) | n/a |
<!-- END_TF_DOCS -->
