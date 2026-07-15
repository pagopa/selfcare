# apim_api

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api"></a> [apim\_api](#module\_apim\_api) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v9.4.0 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_api_management_api_version_set.apim_api_version_set](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_api_dns_zone_prefix"></a> [api\_dns\_zone\_prefix](#input\_api\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"api.selfcare"` | no |
| <a name="input_api_name"></a> [api\_name](#input\_api\_name) | The name of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_api_operation_policies"></a> [api\_operation\_policies](#input\_api\_operation\_policies) | List of api policy for given operation. | <pre>list(object({<br/>    operation_id = string<br/>    xml_content  = string<br/>    }<br/>  ))</pre> | `[]` | no |
| <a name="input_apim_name"></a> [apim\_name](#input\_apim\_name) | The name of the API Management instance. | `string` | n/a | yes |
| <a name="input_apim_rg"></a> [apim\_rg](#input\_apim\_rg) | The name of the resource group in which the API Management instance exists. | `string` | n/a | yes |
| <a name="input_base_path"></a> [base\_path](#input\_base\_path) | The base path of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_display_name"></a> [display\_name](#input\_display\_name) | The display name of the API in the API Management instance. | `string` | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_openapi_path"></a> [openapi\_path](#input\_openapi\_path) | Path to the OpenAPI specification file. | `string` | n/a | yes |
| <a name="input_private_dns_name"></a> [private\_dns\_name](#input\_private\_dns\_name) | The private DNS name of the API in the API Management instance. | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
