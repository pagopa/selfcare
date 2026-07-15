# local-env

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_nat_gateway.nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/nat_gateway) | data source |
| [azurerm_resource_group.nat_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_subscription.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subscription) | data source |
| [azurerm_virtual_network.vnet_selc](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_api_dns_zone_prefix"></a> [api\_dns\_zone\_prefix](#input\_api\_dns\_zone\_prefix) | API DNS zone prefix (e.g. api.dev.selfcare, api-pnpg.selfcare) | `string` | n/a | yes |
| <a name="input_ca_resource_group_name"></a> [ca\_resource\_group\_name](#input\_ca\_resource\_group\_name) | Resource group name of the Container App Environment | `string` | n/a | yes |
| <a name="input_container_app_cpu"></a> [container\_app\_cpu](#input\_container\_app\_cpu) | CPU cores allocated to each container app replica | `number` | `0.5` | no |
| <a name="input_container_app_desired_replicas"></a> [container\_app\_desired\_replicas](#input\_container\_app\_desired\_replicas) | Desired replicas for the cron scale rule | `string` | `"1"` | no |
| <a name="input_container_app_environment_name"></a> [container\_app\_environment\_name](#input\_container\_app\_environment\_name) | Name of the Azure Container App Environment (e.g. selc-d-cae-002) | `string` | n/a | yes |
| <a name="input_container_app_max_replicas"></a> [container\_app\_max\_replicas](#input\_container\_app\_max\_replicas) | Maximum number of container app replicas | `number` | `1` | no |
| <a name="input_container_app_memory"></a> [container\_app\_memory](#input\_container\_app\_memory) | Memory allocated to each container app replica (e.g. 1Gi, 2.5Gi) | `string` | `"1Gi"` | no |
| <a name="input_container_app_min_replicas"></a> [container\_app\_min\_replicas](#input\_container\_app\_min\_replicas) | Minimum number of container app replicas | `number` | `1` | no |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | DNS zone prefix (e.g. dev.selfcare, selfcare, pnpg.dev.selfcare) | `string` | n/a | yes |
| <a name="input_domain"></a> [domain](#input\_domain) | Domain: ar or pnpg | `string` | n/a | yes |
| <a name="input_env"></a> [env](#input\_env) | Environment name: dev, uat, prod | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short code: d, u, p | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | External domain suffix (pagopa.it for ar/dev-pnpg, it for uat/prod-pnpg) | `string` | `"pagopa.it"` | no |
| <a name="input_nat_gw_name"></a> [nat\_gw\_name](#input\_nat\_gw\_name) | NAT gateway name. When omitted, defaults to <project>-nat\_gw. | `string` | `null` | no |
| <a name="input_nat_pip_outbound_name"></a> [nat\_pip\_outbound\_name](#input\_nat\_pip\_outbound\_name) | Outbound public IP name associated to NAT gateway. | `string` | `null` | no |
| <a name="input_nat_rg_name"></a> [nat\_rg\_name](#input\_nat\_rg\_name) | NAT resource group name. When omitted, defaults to <project>-nat-rg. | `string` | `null` | no |
| <a name="input_private_dns_name_domain"></a> [private\_dns\_name\_domain](#input\_private\_dns\_name\_domain) | CAE private DNS domain suffix (e.g. whitemoss-eb7ef327.westeurope.azurecontainerapps.io) | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_config"></a> [config](#output\_config) | n/a |
| <a name="output_key_vault_id"></a> [key\_vault\_id](#output\_key\_vault\_id) | n/a |
| <a name="output_nat_gw_id"></a> [nat\_gw\_id](#output\_nat\_gw\_id) | n/a |
| <a name="output_nat_gw_rg_name"></a> [nat\_gw\_rg\_name](#output\_nat\_gw\_rg\_name) | n/a |
| <a name="output_subscription_id"></a> [subscription\_id](#output\_subscription\_id) | n/a |
| <a name="output_tenant_id"></a> [tenant\_id](#output\_tenant\_id) | n/a |
| <a name="output_vnet_resource_group_name"></a> [vnet\_resource\_group\_name](#output\_vnet\_resource\_group\_name) | n/a |
| <a name="output_vnet_selc_name"></a> [vnet\_selc\_name](#output\_vnet\_selc\_name) | n/a |
<!-- END_TF_DOCS -->
