# uat-pnpg

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
| <a name="module_collection_contract_templates"></a> [collection\_contract\_templates](#module\_collection\_contract\_templates) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_collection_products"></a> [collection\_products](#module\_collection\_products) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_product_ms"></a> [container\_app\_product\_ms](#module\_container\_app\_product\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb"></a> [cosmosdb](#module\_cosmosdb) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | n/a | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
