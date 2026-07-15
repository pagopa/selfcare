# dev-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api_auth"></a> [apim\_api\_auth](#module\_apim\_api\_auth) | ../../_modules/apim_api | n/a |
| <a name="module_collection_auth_otp_flows"></a> [collection\_auth\_otp\_flows](#module\_collection\_auth\_otp\_flows) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_auth_ms"></a> [container\_app\_auth\_ms](#module\_container\_app\_auth\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb_auth"></a> [cosmosdb\_auth](#module\_cosmosdb\_auth) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for auth | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
