# ai_search_onboarding

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_restapi"></a> [restapi](#requirement\_restapi) | 3.0.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_restapi.search"></a> [restapi.search](#provider\_restapi.search) | 3.0.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [restapi_object.search_index](https://registry.terraform.io/providers/mastercard/restapi/3.0.0/docs/resources/object) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_api_version"></a> [api\_version](#input\_api\_version) | API version to use for Azure Search REST API calls | `string` | `"2024-07-01"` | no |
| <a name="input_domain"></a> [domain](#input\_domain) | Domain | `string` | n/a | yes |
| <a name="input_srch_service_name"></a> [srch\_service\_name](#input\_srch\_service\_name) | AI Search service name | `string` | n/a | yes |
| <a name="input_srch_service_primary_key"></a> [srch\_service\_primary\_key](#input\_srch\_service\_primary\_key) | AI Search service primary key | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
