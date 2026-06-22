# events

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
| <a name="module_event_hub"></a> [event\_hub](#module\_event\_hub) | github.com/pagopa/terraform-azurerm-v4.git//eventhub | v9.6.1 |
| <a name="module_eventhub_snet"></a> [eventhub\_snet](#module\_eventhub\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_secret.event_hub_connection_strings](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.event_hub_connection_strings_lc](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.event_hub_keys](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.event_hub_keys_lc](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_private_dns_zone.eventhub](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/private_dns_zone) | resource |
| [azurerm_private_dns_zone_virtual_network_link.eventhub](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/private_dns_zone_virtual_network_link) | resource |
| [azurerm_resource_group.event_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_role_assignment.event_hubs_assignments](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/role_assignment) | resource |
| [azurerm_eventhub.event_hubs](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/eventhub) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_action_group_email_id"></a> [action\_group\_email\_id](#input\_action\_group\_email\_id) | n/a | `string` | n/a | yes |
| <a name="input_action_group_error_id"></a> [action\_group\_error\_id](#input\_action\_group\_error\_id) | From monitor module | `string` | `null` | no |
| <a name="input_action_group_slack_id"></a> [action\_group\_slack\_id](#input\_action\_group\_slack\_id) | n/a | `string` | n/a | yes |
| <a name="input_cidr_subnet_eventhub"></a> [cidr\_subnet\_eventhub](#input\_cidr\_subnet\_eventhub) | EventHub subnet address space | `list(string)` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_eventhub_alerts_enabled"></a> [eventhub\_alerts\_enabled](#input\_eventhub\_alerts\_enabled) | n/a | `bool` | `false` | no |
| <a name="input_eventhub_auto_inflate_enabled"></a> [eventhub\_auto\_inflate\_enabled](#input\_eventhub\_auto\_inflate\_enabled) | n/a | `bool` | `false` | no |
| <a name="input_eventhub_capacity"></a> [eventhub\_capacity](#input\_eventhub\_capacity) | n/a | `number` | `null` | no |
| <a name="input_eventhub_ip_rules"></a> [eventhub\_ip\_rules](#input\_eventhub\_ip\_rules) | n/a | <pre>list(object({<br/>    ip_mask = string<br/>    action  = string<br/>  }))</pre> | `[]` | no |
| <a name="input_eventhub_maximum_throughput_units"></a> [eventhub\_maximum\_throughput\_units](#input\_eventhub\_maximum\_throughput\_units) | n/a | `number` | `null` | no |
| <a name="input_eventhub_metric_alerts"></a> [eventhub\_metric\_alerts](#input\_eventhub\_metric\_alerts) | n/a | <pre>map(object({<br/>    aggregation = string<br/>    metric_name = string<br/>    description = string<br/>    operator    = string<br/>    threshold   = number<br/>    frequency   = string<br/>    window_size = string<br/>    dimension = list(object({<br/>      name     = string<br/>      operator = string<br/>      values   = list(string)<br/>    }))<br/>  }))</pre> | `{}` | no |
| <a name="input_eventhub_sku_name"></a> [eventhub\_sku\_name](#input\_eventhub\_sku\_name) | n/a | `string` | `"Basic"` | no |
| <a name="input_eventhubs"></a> [eventhubs](#input\_eventhubs) | A list of event hub topics to add to namespace. | <pre>list(object({<br/>    name              = string<br/>    partitions        = number<br/>    message_retention = number<br/>    consumers         = list(string)<br/>    keys = list(object({<br/>      name   = string<br/>      listen = bool<br/>      send   = bool<br/>      manage = bool<br/>    }))<br/>    iam_roles = optional(map(string), {})<br/>  }))</pre> | `[]` | no |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | From key\_vault module | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | n/a | `string` | `"Enabled"` | no |
| <a name="input_privatelink_servicebus_windows_net_ids"></a> [privatelink\_servicebus\_windows\_net\_ids](#input\_privatelink\_servicebus\_windows\_net\_ids) | Private DNS zone IDs for servicebus | `list(string)` | n/a | yes |
| <a name="input_privatelink_servicebus_windows_net_names"></a> [privatelink\_servicebus\_windows\_net\_names](#input\_privatelink\_servicebus\_windows\_net\_names) | Private DNS zone names for servicebus | `list(string)` | n/a | yes |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | From network module | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |
| <a name="input_vnet_id"></a> [vnet\_id](#input\_vnet\_id) | n/a | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | n/a | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_event_rg_name"></a> [event\_rg\_name](#output\_event\_rg\_name) | n/a |
| <a name="output_eventhub_namespace_name"></a> [eventhub\_namespace\_name](#output\_eventhub\_namespace\_name) | n/a |
<!-- END_TF_DOCS -->
