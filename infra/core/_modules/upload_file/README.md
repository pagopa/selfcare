# upload_file

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_local"></a> [local](#provider\_local) | 2.9.0 |
| <a name="provider_null"></a> [null](#provider\_null) | 3.3.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [null_resource.upload_resources_logo](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [local_file.resources_file](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_container"></a> [container](#input\_container) | Storage container name | `string` | n/a | yes |
| <a name="input_file_path"></a> [file\_path](#input\_file\_path) | File path | `string` | n/a | yes |
| <a name="input_primary_connection_string"></a> [primary\_connection\_string](#input\_primary\_connection\_string) | Storage account primary connection string | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
