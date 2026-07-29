# prod-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azapi"></a> [azapi](#requirement\_azapi) | > 2.0.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |
| <a name="requirement_dx"></a> [dx](#requirement\_dx) | ~> 0.0 |
| <a name="requirement_random"></a> [random](#requirement\_random) | >= 3.0.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.81.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api"></a> [apim\_api](#module\_apim\_api) | ../../_modules/apim_api | n/a |
| <a name="module_collection_webhook_notifications"></a> [collection\_webhook\_notifications](#module\_collection\_webhook\_notifications) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_collection_webhooks"></a> [collection\_webhooks](#module\_collection\_webhooks) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_webhook_ms"></a> [container\_app\_webhook\_ms](#module\_container\_app\_webhook\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb_webhook"></a> [cosmosdb\_webhook](#module\_cosmosdb\_webhook) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |
| <a name="module_storage_queue"></a> [storage\_queue](#module\_storage\_queue) | ../../_modules/azure_storage_queue | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_user_assigned_identity.cae_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | n/a | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
