# assets

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_null"></a> [null](#provider\_null) | 3.3.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [null_resource.upload_alert_message](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_assets](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_config](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_robots](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_domain"></a> [app\_domain](#input\_app\_domain) | Application domain name (e.g. ar or pnpg) | `string` | n/a | yes |
| <a name="input_checkout_cdn_name"></a> [checkout\_cdn\_name](#input\_checkout\_cdn\_name) | CDN endpoint name | `string` | n/a | yes |
| <a name="input_checkout_cdn_storage_primary_access_key"></a> [checkout\_cdn\_storage\_primary\_access\_key](#input\_checkout\_cdn\_storage\_primary\_access\_key) | CDN storage account primary access key | `string` | n/a | yes |
| <a name="input_checkout_endpoint_name"></a> [checkout\_endpoint\_name](#input\_checkout\_endpoint\_name) | Checkout frontend endpoint name | `string` | n/a | yes |
| <a name="input_checkout_fe_rg_name"></a> [checkout\_fe\_rg\_name](#input\_checkout\_fe\_rg\_name) | Checkout frontend resource group name | `string` | n/a | yes |
| <a name="input_env"></a> [env](#input\_env) | Environment name (e.g. dev, uat, prod) | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
