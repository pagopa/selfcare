# dapr

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | > 4.0.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_redis"></a> [redis](#module\_redis) | github.com/pagopa/terraform-azurerm-v4.git//redis_cache | v7.26.5 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_container_app_environment_dapr_component.appinsight_binding](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_dapr_component) | resource |
| [azurerm_container_app_environment_dapr_component.eventhub_pubsub](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_dapr_component) | resource |
| [azurerm_container_app_environment_dapr_component.redis_state](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_dapr_component) | resource |
| [azurerm_container_app_environment_dapr_component.secrets](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_dapr_component) | resource |
| [azurerm_resource_group.redis_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_container_app_environment.cae](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.event_hub_consumer_key](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_private_dns_zone.privatelink_redis_cache_windows_net](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_resource_group.rg_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_subnet.redis_snet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subnet) | data source |
| [azurerm_user_assigned_identity.cae_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_virtual_network.vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_ca_name"></a> [ca\_name](#input\_ca\_name) | Container App name. Used directly as DAPR app\_id scope (avoids data source lookup that fails on first deploy). | `string` | `"cae-cp"` | no |
| <a name="input_ca_rg_name"></a> [ca\_rg\_name](#input\_ca\_rg\_name) | Deprecated - no longer used. Kept for backward compatibility. | `string` | `"cae-rg"` | no |
| <a name="input_cae_name"></a> [cae\_name](#input\_cae\_name) | Container App Environment name | `string` | `"cae-cp"` | no |
| <a name="input_cae_rg_name"></a> [cae\_rg\_name](#input\_cae\_rg\_name) | Container App Environment Resource group name | `string` | `"cae-rg"` | no |
| <a name="input_cidr_subnet_redis"></a> [cidr\_subnet\_redis](#input\_cidr\_subnet\_redis) | Redis network address space. | `list(string)` | `[]` | no |
| <a name="input_consumer_group"></a> [consumer\_group](#input\_consumer\_group) | Eventhub consumer group | `string` | `"party-proxy"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | `"d"` | no |
| <a name="input_key_vault_event_hub_consumer_key"></a> [key\_vault\_event\_hub\_consumer\_key](#input\_key\_vault\_event\_hub\_consumer\_key) | Name of Key Vault | `string` | n/a | yes |
| <a name="input_key_vault_name"></a> [key\_vault\_name](#input\_key\_vault\_name) | Name of Key Vault | `string` | n/a | yes |
| <a name="input_key_vault_resource_group_name"></a> [key\_vault\_resource\_group\_name](#input\_key\_vault\_resource\_group\_name) | Name of Key Vault resource group | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | Domain prefix | `string` | `"selc"` | no |
| <a name="input_project"></a> [project](#input\_project) | Selfcare prefix and short environment | `string` | n/a | yes |
| <a name="input_queue_consumer_group"></a> [queue\_consumer\_group](#input\_queue\_consumer\_group) | Queue consumer group | `string` | n/a | yes |
| <a name="input_queue_port"></a> [queue\_port](#input\_queue\_port) | Queue base url port | `string` | n/a | yes |
| <a name="input_queue_topic"></a> [queue\_topic](#input\_queue\_topic) | Queue topic | `string` | n/a | yes |
| <a name="input_queue_url"></a> [queue\_url](#input\_queue\_url) | Queue base url | `string` | n/a | yes |
| <a name="input_redis_capacity"></a> [redis\_capacity](#input\_redis\_capacity) | n/a | `number` | `0` | no |
| <a name="input_redis_enable"></a> [redis\_enable](#input\_redis\_enable) | n/a | `bool` | `false` | no |
| <a name="input_redis_family"></a> [redis\_family](#input\_redis\_family) | n/a | `string` | `"C"` | no |
| <a name="input_redis_private_endpoint_enabled"></a> [redis\_private\_endpoint\_enabled](#input\_redis\_private\_endpoint\_enabled) | n/a | `bool` | `true` | no |
| <a name="input_redis_sku_name"></a> [redis\_sku\_name](#input\_redis\_sku\_name) | n/a | `string` | `"Basic"` | no |
| <a name="input_redis_version"></a> [redis\_version](#input\_redis\_version) | n/a | `number` | `6` | no |
| <a name="input_search_service_key"></a> [search\_service\_key](#input\_search\_service\_key) | Key of ai search service | `string` | n/a | yes |
| <a name="input_search_service_name"></a> [search\_service\_name](#input\_search\_service\_name) | Name of ai search service | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
