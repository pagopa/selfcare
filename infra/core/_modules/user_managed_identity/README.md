# user_managed_identity

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_management_lock.product_storage_blob_identity_lock](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_management_lock.product_storage_table_identity_lock](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_resource_group.user_managed_identity_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_role_assignment.product_storage_blob_identity_role_assignment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/role_assignment) | resource |
| [azurerm_role_assignment.product_storage_table_identity_role_assignment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/role_assignment) | resource |
| [azurerm_user_assigned_identity.product_storage_blob_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/user_assigned_identity) | resource |
| [azurerm_user_assigned_identity.product_storage_table_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/user_assigned_identity) | resource |
| [azurerm_storage_account.product_storage_table](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_domain"></a> [domain](#input\_domain) | Domain name for resource naming | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Short environment name | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | Azure region | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | Prefix for resource names | `string` | `"selc"` | no |
| <a name="input_product_storage_name"></a> [product\_storage\_name](#input\_product\_storage\_name) | Complete name for the product storage | `string` | n/a | yes |
| <a name="input_product_storage_rg"></a> [product\_storage\_rg](#input\_product\_storage\_rg) | Resource group name for the product storage | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | Resource tags | `map(any)` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_product_storage_blob_identity_client_id"></a> [product\_storage\_blob\_identity\_client\_id](#output\_product\_storage\_blob\_identity\_client\_id) | Client ID of the user-assigned identity with Storage Blob Data Contributor role |
| <a name="output_product_storage_blob_identity_id"></a> [product\_storage\_blob\_identity\_id](#output\_product\_storage\_blob\_identity\_id) | ID of the user-assigned identity with Storage Blob Data Contributor role |
| <a name="output_product_storage_table_identity_id"></a> [product\_storage\_table\_identity\_id](#output\_product\_storage\_table\_identity\_id) | ID of the user-assigned identity with Storage Table Data Contributor role |
<!-- END_TF_DOCS -->
