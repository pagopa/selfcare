# appgateway

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
| <a name="module_app_gw"></a> [app\_gw](#module\_app\_gw) | github.com/pagopa/terraform-azurerm-v4.git//app_gateway | v9.6.1 |
| <a name="module_appgateway_snet"></a> [appgateway\_snet](#module\_appgateway\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.app_gateway_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_user_assigned_identity.appgateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/user_assigned_identity) | resource |
| [azurerm_api_management.this](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/api_management) | data source |
| [azurerm_key_vault_certificate.api_pnpg_selfcare_certificate](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_certificate) | data source |
| [azurerm_key_vault_certificate.app_gw_platform](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_certificate) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_action_group_email_id"></a> [action\_group\_email\_id](#input\_action\_group\_email\_id) | n/a | `string` | n/a | yes |
| <a name="input_action_group_error_id"></a> [action\_group\_error\_id](#input\_action\_group\_error\_id) | From key\_vault module From monitor module | `string` | `null` | no |
| <a name="input_action_group_slack_id"></a> [action\_group\_slack\_id](#input\_action\_group\_slack\_id) | n/a | `string` | n/a | yes |
| <a name="input_aks_platform_env"></a> [aks\_platform\_env](#input\_aks\_platform\_env) | n/a | `string` | n/a | yes |
| <a name="input_app_gateway_alerts_enabled"></a> [app\_gateway\_alerts\_enabled](#input\_app\_gateway\_alerts\_enabled) | n/a | `bool` | `false` | no |
| <a name="input_app_gateway_api_certificate_name"></a> [app\_gateway\_api\_certificate\_name](#input\_app\_gateway\_api\_certificate\_name) | n/a | `string` | n/a | yes |
| <a name="input_app_gateway_api_pnpg_certificate_name"></a> [app\_gateway\_api\_pnpg\_certificate\_name](#input\_app\_gateway\_api\_pnpg\_certificate\_name) | n/a | `string` | n/a | yes |
| <a name="input_app_gateway_max_capacity"></a> [app\_gateway\_max\_capacity](#input\_app\_gateway\_max\_capacity) | n/a | `number` | `2` | no |
| <a name="input_app_gateway_min_capacity"></a> [app\_gateway\_min\_capacity](#input\_app\_gateway\_min\_capacity) | n/a | `number` | `0` | no |
| <a name="input_app_gateway_sku_name"></a> [app\_gateway\_sku\_name](#input\_app\_gateway\_sku\_name) | n/a | `string` | `"Standard_v2"` | no |
| <a name="input_app_gateway_sku_tier"></a> [app\_gateway\_sku\_tier](#input\_app\_gateway\_sku\_tier) | n/a | `string` | `"Standard_v2"` | no |
| <a name="input_app_gateway_waf_enabled"></a> [app\_gateway\_waf\_enabled](#input\_app\_gateway\_waf\_enabled) | n/a | `bool` | `false` | no |
| <a name="input_appgateway_public_ip_id"></a> [appgateway\_public\_ip\_id](#input\_appgateway\_public\_ip\_id) | n/a | `string` | n/a | yes |
| <a name="input_auth_ms_private_dns_suffix"></a> [auth\_ms\_private\_dns\_suffix](#input\_auth\_ms\_private\_dns\_suffix) | n/a | `string` | n/a | yes |
| <a name="input_ca_pnpg_suffix_dns_private_name"></a> [ca\_pnpg\_suffix\_dns\_private\_name](#input\_ca\_pnpg\_suffix\_dns\_private\_name) | n/a | `string` | n/a | yes |
| <a name="input_cidr_subnet_appgateway"></a> [cidr\_subnet\_appgateway](#input\_cidr\_subnet\_appgateway) | n/a | `list(string)` | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | n/a | `string` | `"selfcare"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | n/a | `string` | `"pagopa.it"` | no |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | From key\_vault module | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | n/a | `string` | `"Enabled"` | no |
| <a name="input_rg_vnet_location"></a> [rg\_vnet\_location](#input\_rg\_vnet\_location) | n/a | `string` | n/a | yes |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | From network module | `string` | n/a | yes |
| <a name="input_sec_rg_location"></a> [sec\_rg\_location](#input\_sec\_rg\_location) | KV resource group location, used to store appgateway secrets for sec environment | `string` | n/a | yes |
| <a name="input_sec_rg_name"></a> [sec\_rg\_name](#input\_sec\_rg\_name) | KV resource group name, used to store appgateway secrets for sec environment | `string` | n/a | yes |
| <a name="input_spid_pnpg_path_prefix"></a> [spid\_pnpg\_path\_prefix](#input\_spid\_pnpg\_path\_prefix) | n/a | `string` | `"/spid/v1"` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Tenant Id | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | n/a | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_appgateway_name"></a> [appgateway\_name](#output\_appgateway\_name) | n/a |
<!-- END_TF_DOCS -->
