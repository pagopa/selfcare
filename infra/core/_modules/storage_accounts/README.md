# storage_accounts

<!-- BEGIN_TF_DOCS -->

## Requirements

No requirements.

## Providers

| Name                                                          | Version |
|---------------------------------------------------------------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.18.0  |

## Modules

| Name                                                                                              | Source                                                  | Version |
|---------------------------------------------------------------------------------------------------|---------------------------------------------------------|---------|
| <a name="module_com_st"></a> [com\_st](#module\_com\_st)                                          | pagopa-dx/azure-storage-account/azurerm                 | 0.0.9   |
| <a name="module_storage_api"></a> [storage\_api](#module\_storage\_api)                           | github.com/pagopa/terraform-azurerm-v4//storage_account | v1.2.1  |
| <a name="module_storage_api_events"></a> [storage\_api\_events](#module\_storage\_api\_events)    | github.com/pagopa/terraform-azurerm-v4//storage_account | v1.2.1  |
| <a name="module_storage_api_replica"></a> [storage\_api\_replica](#module\_storage\_api\_replica) | github.com/pagopa/terraform-azurerm-v4//storage_account | v1.2.1  |

## Resources

| Name                                                                                                                                                                                         | Type     |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| [azurerm_monitor_metric_alert.iopstapi_throttling_low_availability](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_metric_alert)                    | resource |
| [azurerm_storage_container.cached](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container)                                                        | resource |
| [azurerm_storage_container.deleted_messages_logs](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container)                                         | resource |
| [azurerm_storage_container.message_content](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container)                                               | resource |
| [azurerm_storage_container.operations](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container)                                                    | resource |
| [azurerm_storage_container_immutability_policy.deleted_messages_logs](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container_immutability_policy) | resource |
| [azurerm_storage_object_replication.api_replica](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_object_replication)                                 | resource |
| [azurerm_storage_queue.delete_messages](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                                       | resource |
| [azurerm_storage_queue.events](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                                                | resource |
| [azurerm_storage_queue.message_paymentupdater_failures](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                       | resource |
| [azurerm_storage_queue.push_notif_notifymessage](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                              | resource |
| [azurerm_storage_queue.push_notif_notifymessage_poison](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                       | resource |
| [azurerm_storage_queue.push_notifications](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                                    | resource |
| [azurerm_storage_queue.push_notifications_poison](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_queue)                                             | resource |
| [azurerm_storage_table.faileduserdataprocessing](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                                              | resource |
| [azurerm_storage_table.message-statuses-dataplan-ingestion-errors](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                            | resource |
| [azurerm_storage_table.messages-dataplan-ingestion-errors](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                                    | resource |
| [azurerm_storage_table.messages_ingestion_error](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                                              | resource |
| [azurerm_storage_table.subscriptionsfeedbyday](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                                                | resource |
| [azurerm_storage_table.validationtokens](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_table)                                                      | resource |

## Inputs

| Name                                                                                                                   | Description                                     | Type                                                                                                          | Default | Required |
|------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment)                                                    | n/a                                             | <pre>object({<br/>    prefix    = string<br/>    env_short = string<br/>    location  = string<br/>  })</pre> | n/a     |   yes    |
| <a name="input_error_action_group_id"></a> [error\_action\_group\_id](#input\_error\_action\_group\_id)                | n/a                                             | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_legacy_resource_group_name"></a> [legacy\_resource\_group\_name](#input\_legacy\_resource\_group\_name) | Resource group name for VNet                    | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_location"></a> [location](#input\_location)                                                             | Azure region                                    | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_location_short"></a> [location\_short](#input\_location\_short)                                         | Azure region short name                         | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_project"></a> [project](#input\_project)                                                                | IO prefix, short environment and short location | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_project_legacy"></a> [project\_legacy](#input\_project\_legacy)                                         | IO prefix and short environment                 | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name)                        | Resource group name for VNet                    | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_subnet_pep_id"></a> [subnet\_pep\_id](#input\_subnet\_pep\_id)                                          | n/a                                             | `string`                                                                                                      | n/a     |   yes    |
| <a name="input_tags"></a> [tags](#input\_tags)                                                                         | Resource tags                                   | `map(any)`                                                                                                    | n/a     |   yes    |

## Outputs

| Name                                                                                                       | Description |
|------------------------------------------------------------------------------------------------------------|-------------|
| <a name="output_com_st_connectiostring"></a> [com\_st\_connectiostring](#output\_com\_st\_connectiostring) | n/a         |
| <a name="output_com_st_id"></a> [com\_st\_id](#output\_com\_st\_id)                                        | n/a         |
| <a name="output_com_st_name"></a> [com\_st\_name](#output\_com\_st\_name)                                  | n/a         |
| <a name="output_com_st_rg"></a> [com\_st\_rg](#output\_com\_st\_rg)                                        | n/a         |

<!-- END_TF_DOCS -->
