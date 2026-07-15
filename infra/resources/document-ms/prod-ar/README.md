# prod-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azapi"></a> [azapi](#requirement\_azapi) | > 2.0.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_collection_documents"></a> [collection\_documents](#module\_collection\_documents) | ../../_modules/cosmosdb_collection | n/a |
| <a name="module_container_app_document_ms"></a> [container\_app\_document\_ms](#module\_container\_app\_document\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_cosmosdb_document"></a> [cosmosdb\_document](#module\_cosmosdb\_document) | ../../_modules/cosmosdb_database | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for document-ms container app | `string` | `"latest"` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_container_app_document_ms_name"></a> [container\_app\_document\_ms\_name](#output\_container\_app\_document\_ms\_name) | n/a |
| <a name="output_documents_collection_id"></a> [documents\_collection\_id](#output\_documents\_collection\_id) | n/a |
<!-- END_TF_DOCS -->
