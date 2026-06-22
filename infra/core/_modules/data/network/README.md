# network

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
| [azurerm_private_dns_zone.privatelink_blob_core_windows_net](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_private_dns_zone.privatelink_redis_cache_windows_net_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_subnet.private_endpoint_subnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subnet) | data source |
| [azurerm_virtual_network.vnet_core](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_redis_private_endpoint_enabled"></a> [redis\_private\_endpoint\_enabled](#input\_redis\_private\_endpoint\_enabled) | n/a | `bool` | `true` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_private_endpoint_subnet"></a> [private\_endpoint\_subnet](#output\_private\_endpoint\_subnet) | n/a |
| <a name="output_privatelink_blob_core_windows_net"></a> [privatelink\_blob\_core\_windows\_net](#output\_privatelink\_blob\_core\_windows\_net) | n/a |
| <a name="output_privatelink_mongo_cosmos_azure_com"></a> [privatelink\_mongo\_cosmos\_azure\_com](#output\_privatelink\_mongo\_cosmos\_azure\_com) | n/a |
| <a name="output_privatelink_redis_cache_windows_net_vnet"></a> [privatelink\_redis\_cache\_windows\_net\_vnet](#output\_privatelink\_redis\_cache\_windows\_net\_vnet) | n/a |
| <a name="output_vnet_core"></a> [vnet\_core](#output\_vnet\_core) | n/a |
<!-- END_TF_DOCS -->
