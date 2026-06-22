# vpn

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_random"></a> [random](#provider\_random) | 3.9.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_dns_forwarder"></a> [dns\_forwarder](#module\_dns\_forwarder) | git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_vm_image | v9.6.1 |
| <a name="module_dns_forwarder_pair_subnet"></a> [dns\_forwarder\_pair\_subnet](#module\_dns\_forwarder\_pair\_subnet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |
| <a name="module_dns_forwarder_pair_vpn"></a> [dns\_forwarder\_pair\_vpn](#module\_dns\_forwarder\_pair\_vpn) | git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_scale_set_vm | v9.6.1 |
| <a name="module_dns_forwarder_snet"></a> [dns\_forwarder\_snet](#module\_dns\_forwarder\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |
| <a name="module_dns_forwarder_vpn"></a> [dns\_forwarder\_vpn](#module\_dns\_forwarder\_vpn) | git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_scale_set_vm | v9.6.1 |
| <a name="module_vpn"></a> [vpn](#module\_vpn) | github.com/pagopa/terraform-azurerm-v4.git//vpn_gateway | v9.6.1 |
| <a name="module_vpn_pair_dns_forwarder"></a> [vpn\_pair\_dns\_forwarder](#module\_vpn\_pair\_dns\_forwarder) | git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_vm_image | v9.6.1 |
| <a name="module_vpn_snet"></a> [vpn\_snet](#module\_vpn\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [random_id.pair_dns_forwarder_hash](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/id) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_cidr_subnet_dns_forwarder"></a> [cidr\_subnet\_dns\_forwarder](#input\_cidr\_subnet\_dns\_forwarder) | CIDR block for the DNS forwarder subnet | `list(string)` | n/a | yes |
| <a name="input_cidr_subnet_pair_dnsforwarder"></a> [cidr\_subnet\_pair\_dnsforwarder](#input\_cidr\_subnet\_pair\_dnsforwarder) | CIDR block for the paired DNS forwarder subnet | `list(string)` | n/a | yes |
| <a name="input_cidr_subnet_vpn"></a> [cidr\_subnet\_vpn](#input\_cidr\_subnet\_vpn) | CIDR block for the VPN subnet | `list(string)` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Short environment name (e.g., d, p) | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | n/a | yes |
| <a name="input_location_pair"></a> [location\_pair](#input\_location\_pair) | n/a | `string` | n/a | yes |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | Private endpoint network policies | `string` | `"Enabled"` | no |
| <a name="input_project"></a> [project](#input\_project) | n/a | `string` | n/a | yes |
| <a name="input_project_pair"></a> [project\_pair](#input\_project\_pair) | n/a | `string` | n/a | yes |
| <a name="input_rg_pair_vnet_name"></a> [rg\_pair\_vnet\_name](#input\_rg\_pair\_vnet\_name) | Resource group name for the paired VNet | `string` | n/a | yes |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | Resource group name for the VNet (for DNS zone) | `string` | n/a | yes |
| <a name="input_sec_storage_id"></a> [sec\_storage\_id](#input\_sec\_storage\_id) | Storage account ID for security logs | `string` | n/a | yes |
| <a name="input_sec_workspace_id"></a> [sec\_workspace\_id](#input\_sec\_workspace\_id) | Log Analytics Workspace ID for security logs | `string` | n/a | yes |
| <a name="input_subscription_id"></a> [subscription\_id](#input\_subscription\_id) | Subscription ID for the paired VNet | `string` | n/a | yes |
| <a name="input_subscription_name"></a> [subscription\_name](#input\_subscription\_name) | Subscription name for the paired VNet | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Tenant ID for the VPN gateway | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | VNet name for the VNet (for DNS zone) | `string` | n/a | yes |
| <a name="input_vnet_pair_name"></a> [vnet\_pair\_name](#input\_vnet\_pair\_name) | VNet name for the paired VNet | `string` | n/a | yes |
| <a name="input_vpn_app_client_id"></a> [vpn\_app\_client\_id](#input\_vpn\_app\_client\_id) | VPN APP client ID | `string` | n/a | yes |
| <a name="input_vpn_pip_sku"></a> [vpn\_pip\_sku](#input\_vpn\_pip\_sku) | SKU for the VPN Public IP | `string` | `"Standard"` | no |
| <a name="input_vpn_sku"></a> [vpn\_sku](#input\_vpn\_sku) | SKU for the VPN gateway | `string` | `"VpnGw1"` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_subnet_pair_id"></a> [subnet\_pair\_id](#output\_subnet\_pair\_id) | n/a |
<!-- END_TF_DOCS -->
