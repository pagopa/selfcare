# dev-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api"></a> [apim\_api](#module\_apim\_api) | ../../_modules/apim_api | n/a |
| <a name="module_collection_webhook_notifications"></a> [collection\_webhook\_notifications](#module\_collection\_webhook\_notifications) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_collection_webhooks"></a> [collection\_webhooks](#module\_collection\_webhooks) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_webhook_ms"></a> [container\_app\_webhook\_ms](#module\_container\_app\_webhook\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb_webhook"></a> [cosmosdb\_webhook](#module\_cosmosdb\_webhook) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for webhook | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
