# redis

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
| <a name="module_redis"></a> [redis](#module\_redis) | github.com/pagopa/terraform-azurerm-v4.git//redis_cache | v9.6.1 |
| <a name="module_redis_snet"></a> [redis\_snet](#module\_redis\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_secret.redis_primary_access_key](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_cidr_subnet_redis"></a> [cidr\_subnet\_redis](#input\_cidr\_subnet\_redis) | CIDR block for Redis subnet | `list(string)` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | The ID of the Key Vault to use for Redis | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | Private endpoint network policies for subnets | `string` | `"Enabled"` | no |
| <a name="input_privatelink_redis_cache_windows_net_ids"></a> [privatelink\_redis\_cache\_windows\_net\_ids](#input\_privatelink\_redis\_cache\_windows\_net\_ids) | Private DNS Zone IDs for privatelink.redis.cache.windows.net | `list(string)` | `[]` | no |
| <a name="input_project"></a> [project](#input\_project) | n/a | `string` | `"selc"` | no |
| <a name="input_redis_capacity"></a> [redis\_capacity](#input\_redis\_capacity) | The size of the Redis cache to deploy. Valid values are: 0 (for Basic C0), 1 (for Basic C1), 2 (for Standard S1), 3 (for Standard S2), 4 (for Standard S3), 5 (for Premium P1), 6 (for Premium P2), 7 (for Premium P3), 8 (for Premium P4), 9 (for Premium P5), 10 (for Premium P6), 11 (for Premium P7), 12 (for Premium P8), 13 (for Premium P9), and 14 (for Premium P10). | `number` | `1` | no |
| <a name="input_redis_family"></a> [redis\_family](#input\_redis\_family) | n/a | `string` | `"C"` | no |
| <a name="input_redis_private_endpoint_enabled"></a> [redis\_private\_endpoint\_enabled](#input\_redis\_private\_endpoint\_enabled) | n/a | `bool` | `true` | no |
| <a name="input_redis_sku_name"></a> [redis\_sku\_name](#input\_redis\_sku\_name) | n/a | `string` | `"Standard"` | no |
| <a name="input_redis_version"></a> [redis\_version](#input\_redis\_version) | The version of Redis to deploy. Valid values are: 4, 5, and 6. | `string` | `"6"` | no |
| <a name="input_rg_redis"></a> [rg\_redis](#input\_rg\_redis) | n/a | `string` | n/a | yes |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | n/a | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |
| <a name="input_vnet_id"></a> [vnet\_id](#input\_vnet\_id) | n/a | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | n/a | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
