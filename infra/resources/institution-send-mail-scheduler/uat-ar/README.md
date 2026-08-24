# uat-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_container_app_job_institution_send_mail_scheduler"></a> [container\_app\_job\_institution\_send\_mail\_scheduler](#module\_container\_app\_job\_institution\_send\_mail\_scheduler) | ../../_modules/container_app_job | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_storage_account.documents_storage](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |
| [azurerm_storage_account.product_storage](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |
| [azurerm_user_assigned_identity.documents_storage_blob_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.product_storage_blob_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
