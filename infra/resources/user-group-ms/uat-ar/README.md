# uat-ar

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
| <a name="module_collection_user_groups"></a> [collection\_user\_groups](#module\_collection\_user\_groups) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_user_group_ms"></a> [container\_app\_user\_group\_ms](#module\_container\_app\_user\_group\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb"></a> [cosmosdb](#module\_cosmosdb) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
