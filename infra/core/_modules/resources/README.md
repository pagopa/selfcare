# resources

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
| [null_resource.upload_health_json](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_metadata](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_product_institution_types](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_aggregates](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_anac_data_csv](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_default_product_logo](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_default_product_resources_depict-image](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_ivass_data_csv](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_products_logo](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_resources_templates](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [local_file.resources_anac_data_csv](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |
| [local_file.resources_default_product_depict-image](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |
| [local_file.resources_default_product_logo](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |
| [local_file.resources_ivass_data_csv](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_domain"></a> [app\_domain](#input\_app\_domain) | Application domain name (e.g. ar or pnpg) | `string` | n/a | yes |
| <a name="input_checkout_cdn_name"></a> [checkout\_cdn\_name](#input\_checkout\_cdn\_name) | CDN endpoint name (module.checkout\_cdn.name) | `string` | n/a | yes |
| <a name="input_checkout_cdn_storage_primary_access_key"></a> [checkout\_cdn\_storage\_primary\_access\_key](#input\_checkout\_cdn\_storage\_primary\_access\_key) | CDN storage account primary access key (module.checkout\_cdn.storage\_primary\_access\_key) | `string` | n/a | yes |
| <a name="input_checkout_endpoint_name"></a> [checkout\_endpoint\_name](#input\_checkout\_endpoint\_name) | Checkout frontend endpoint name | `string` | n/a | yes |
| <a name="input_checkout_fe_rg_name"></a> [checkout\_fe\_rg\_name](#input\_checkout\_fe\_rg\_name) | Checkout frontend resource group name (azurerm\_resource\_group.checkout\_fe\_rg.name) | `string` | n/a | yes |
| <a name="input_env"></a> [env](#input\_env) | Environment name (e.g. dev, uat, prod) | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Short environment name (e.g. d, u, p) | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
