# storage_accounts

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | n/a |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_storage_account"></a> [storage\_account](#module\_storage\_account) | pagopa-dx/azure-storage-account/azurerm | ~>1.0 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_secret.selc_documents_storage_connection_string](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_management_lock.selc_documents_blob_management_lock](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_management_lock.selc_documents_storage_management_lock](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_storage_container.selc_documents_blob](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container) | resource |
| [azurerm_storage_management_policy.lifecycle](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_management_policy) | resource |
| [azurerm_subnet.documents_snet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet) | resource |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_virtual_network.vnet_selc](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_name"></a> [app\_name](#input\_app\_name) | App name | `string` | n/a | yes |
| <a name="input_base_blob_tier_to_cold_after_days_since_creation_greater_than"></a> [base\_blob\_tier\_to\_cold\_after\_days\_since\_creation\_greater\_than](#input\_base\_blob\_tier\_to\_cold\_after\_days\_since\_creation\_greater\_than) | n/a | `number` | n/a | yes |
| <a name="input_base_blob_tier_to_cool_after_days_since_modification_greater_than"></a> [base\_blob\_tier\_to\_cool\_after\_days\_since\_modification\_greater\_than](#input\_base\_blob\_tier\_to\_cool\_after\_days\_since\_modification\_greater\_than) | n/a | `number` | n/a | yes |
| <a name="input_base_delete_after_days_since_creation_greater_than"></a> [base\_delete\_after\_days\_since\_creation\_greater\_than](#input\_base\_delete\_after\_days\_since\_creation\_greater\_than) | n/a | `number` | n/a | yes |
| <a name="input_blob_features"></a> [blob\_features](#input\_blob\_features) | Advanced blob features like versioning, change feed, immutability, and retention policies. | <pre>object({<br/>    restore_policy_days   = optional(number, 0)<br/>    delete_retention_days = optional(number, 0)<br/>    last_access_time      = optional(bool, false)<br/>    versioning            = optional(bool, false)<br/>    change_feed = optional(object({<br/>      enabled           = optional(bool, false)<br/>      retention_in_days = optional(number, 0)<br/>    }), { enabled = false })<br/>    immutability_policy = optional(object({<br/>      enabled                       = optional(bool, false)<br/>      allow_protected_append_writes = optional(bool, false)<br/>      period_since_creation_in_days = optional(number, 730)<br/>    }), { enabled = false })<br/>  })</pre> | <pre>{<br/>  "change_feed": {<br/>    "enabled": false,<br/>    "retention_in_days": 0<br/>  },<br/>  "delete_retention_days": 0,<br/>  "immutability_policy": {<br/>    "enabled": false<br/>  },<br/>  "last_access_time": false,<br/>  "restore_policy_days": 0,<br/>  "versioning": false<br/>}</pre> | no |
| <a name="input_cidr_subnet_contract_storage"></a> [cidr\_subnet\_contract\_storage](#input\_cidr\_subnet\_contract\_storage) | Documents storage address space. | `list(string)` | n/a | yes |
| <a name="input_domain"></a> [domain](#input\_domain) | Domain | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_instance_number"></a> [instance\_number](#input\_instance\_number) | The istance number to create | `string` | n/a | yes |
| <a name="input_key_vault_name"></a> [key\_vault\_name](#input\_key\_vault\_name) | Name of Key Vault | `string` | n/a | yes |
| <a name="input_key_vault_resource_group_name"></a> [key\_vault\_resource\_group\_name](#input\_key\_vault\_resource\_group\_name) | Name of Key Vault resource group | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | Domain prefix | `string` | `"selc"` | no |
| <a name="input_private_dns_zone_resource_group_name"></a> [private\_dns\_zone\_resource\_group\_name](#input\_private\_dns\_zone\_resource\_group\_name) | he name of the resource group holding private DNS zone to use for private endpoints. Default is Virtual Network resource group | `string` | n/a | yes |
| <a name="input_project"></a> [project](#input\_project) | Selfcare prefix and short environment | `string` | n/a | yes |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name) | Resource group | `string` | n/a | yes |
| <a name="input_snapshot_change_tier_to_cool_after_days_since_creation"></a> [snapshot\_change\_tier\_to\_cool\_after\_days\_since\_creation](#input\_snapshot\_change\_tier\_to\_cool\_after\_days\_since\_creation) | n/a | `number` | n/a | yes |
| <a name="input_snapshot_delete_after_days_since_creation_greater_than"></a> [snapshot\_delete\_after\_days\_since\_creation\_greater\_than](#input\_snapshot\_delete\_after\_days\_since\_creation\_greater\_than) | n/a | `number` | n/a | yes |
| <a name="input_suffix_increment"></a> [suffix\_increment](#input\_suffix\_increment) | Suffix increment Container App Environment name | `string` | `""` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_version_change_tier_to_cool_after_days_since_creation"></a> [version\_change\_tier\_to\_cool\_after\_days\_since\_creation](#input\_version\_change\_tier\_to\_cool\_after\_days\_since\_creation) | n/a | `number` | n/a | yes |
| <a name="input_version_delete_after_days_since_creation"></a> [version\_delete\_after\_days\_since\_creation](#input\_version\_delete\_after\_days\_since\_creation) | n/a | `number` | n/a | yes |
| <a name="input_virtual_network_name"></a> [virtual\_network\_name](#input\_virtual\_network\_name) | Name of the resource where resources will be created | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_storage_account"></a> [storage\_account](#output\_storage\_account) | n/a |
| <a name="output_storage_container_name"></a> [storage\_container\_name](#output\_storage\_container\_name) | n/a |
<!-- END_TF_DOCS -->
