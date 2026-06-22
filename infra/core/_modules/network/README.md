# network

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
| <a name="module_private_endpoints_subnet"></a> [private\_endpoints\_subnet](#module\_private\_endpoints\_subnet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |
| <a name="module_vnet"></a> [vnet](#module\_vnet) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network | v9.6.1 |
| <a name="module_vnet_aks_platform"></a> [vnet\_aks\_platform](#module\_vnet\_aks\_platform) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network | v9.6.1 |
| <a name="module_vnet_pair"></a> [vnet\_pair](#module\_vnet\_pair) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network | v9.6.1 |
| <a name="module_vnet_peering_core_2_aks"></a> [vnet\_peering\_core\_2\_aks](#module\_vnet\_peering\_core\_2\_aks) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering | v9.6.1 |
| <a name="module_vnet_peering_pair_vs_aks"></a> [vnet\_peering\_pair\_vs\_aks](#module\_vnet\_peering\_pair\_vs\_aks) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering | v9.6.1 |
| <a name="module_vnet_peering_pair_vs_core"></a> [vnet\_peering\_pair\_vs\_core](#module\_vnet\_peering\_pair\_vs\_core) | github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_public_ip.appgateway_public_ip](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/public_ip) | resource |
| [azurerm_public_ip.outbound_ip_aks_platform](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/public_ip) | resource |
| [azurerm_resource_group.rg_pair_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_resource_group.rg_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_resource_group.rg_vnet_aks](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_aks_platform_env"></a> [aks\_platform\_env](#input\_aks\_platform\_env) | The env name used into AKS platform folder. | `string` | n/a | yes |
| <a name="input_cidr_aks_platform_vnet"></a> [cidr\_aks\_platform\_vnet](#input\_cidr\_aks\_platform\_vnet) | VNet for AKS platform. | `list(string)` | n/a | yes |
| <a name="input_cidr_pair_vnet"></a> [cidr\_pair\_vnet](#input\_cidr\_pair\_vnet) | Virtual network pair address space. | `list(string)` | n/a | yes |
| <a name="input_cidr_subnet_private_endpoints"></a> [cidr\_subnet\_private\_endpoints](#input\_cidr\_subnet\_private\_endpoints) | Private endpoints address space. | `list(string)` | n/a | yes |
| <a name="input_cidr_vnet"></a> [cidr\_vnet](#input\_cidr\_vnet) | Virtual network address space. | `list(string)` | n/a | yes |
| <a name="input_ddos_protection_plan"></a> [ddos\_protection\_plan](#input\_ddos\_protection\_plan) | n/a | <pre>object({<br/>    id     = string<br/>    enable = bool<br/>  })</pre> | `null` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_location_pair"></a> [location\_pair](#input\_location\_pair) | n/a | `string` | n/a | yes |
| <a name="input_location_pair_short"></a> [location\_pair\_short](#input\_location\_pair\_short) | n/a | `string` | n/a | yes |
| <a name="input_location_short"></a> [location\_short](#input\_location\_short) | n/a | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | n/a | `string` | `"Enabled"` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_appgateway_public_ip_address"></a> [appgateway\_public\_ip\_address](#output\_appgateway\_public\_ip\_address) | n/a |
| <a name="output_appgateway_public_ip_id"></a> [appgateway\_public\_ip\_id](#output\_appgateway\_public\_ip\_id) | App Gateway Public IP |
| <a name="output_outbound_ip_aks_platform_id"></a> [outbound\_ip\_aks\_platform\_id](#output\_outbound\_ip\_aks\_platform\_id) | n/a |
| <a name="output_private_endpoints_subnet_id"></a> [private\_endpoints\_subnet\_id](#output\_private\_endpoints\_subnet\_id) | n/a |
| <a name="output_rg_pair_vnet_name"></a> [rg\_pair\_vnet\_name](#output\_rg\_pair\_vnet\_name) | Pair VNet |
| <a name="output_rg_vnet_aks_location"></a> [rg\_vnet\_aks\_location](#output\_rg\_vnet\_aks\_location) | n/a |
| <a name="output_rg_vnet_aks_name"></a> [rg\_vnet\_aks\_name](#output\_rg\_vnet\_aks\_name) | AKS VNet |
| <a name="output_rg_vnet_location"></a> [rg\_vnet\_location](#output\_rg\_vnet\_location) | n/a |
| <a name="output_rg_vnet_name"></a> [rg\_vnet\_name](#output\_rg\_vnet\_name) | Core VNet |
| <a name="output_vnet_aks_platform_id"></a> [vnet\_aks\_platform\_id](#output\_vnet\_aks\_platform\_id) | n/a |
| <a name="output_vnet_aks_platform_name"></a> [vnet\_aks\_platform\_name](#output\_vnet\_aks\_platform\_name) | n/a |
| <a name="output_vnet_id"></a> [vnet\_id](#output\_vnet\_id) | n/a |
| <a name="output_vnet_name"></a> [vnet\_name](#output\_vnet\_name) | n/a |
| <a name="output_vnet_pair_id"></a> [vnet\_pair\_id](#output\_vnet\_pair\_id) | n/a |
| <a name="output_vnet_pair_name"></a> [vnet\_pair\_name](#output\_vnet\_pair\_name) | n/a |
<!-- END_TF_DOCS -->
