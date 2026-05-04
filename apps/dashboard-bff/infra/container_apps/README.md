# Container App

Deploy container on a Container App instance.

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.6.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.85.0 |

## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_apim_api_bff_dashboard"></a> [apim\_api\_bff\_dashboard](#module\_apim\_api\_bff\_dashboard) | github.com/pagopa/terraform-azurerm-v3.git//api_management_api | v8.18.0 |
| <a name="module_container_app_dashboard_backend"></a> [container\_app\_dashboard\_backend](#module\_container\_app\_dashboard\_backend) | github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice | main |

## Resources

| Name | Type |
|------|------|
| [azurerm_api_management_api_version_set.apim_api_bff_dashboard](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_api_dns_zone_prefix"></a> [api\_dns\_zone\_prefix](#input\_api\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"api.selfcare"` | no |
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | n/a | <pre>list(object({<br>    name  = string<br>    value = string<br>  }))</pre> | n/a | yes |
| <a name="input_cae_name"></a> [cae\_name](#input\_cae\_name) | Container App Environment name | `string` | `"cae-cp"` | no |
| <a name="input_container_app"></a> [container\_app](#input\_container\_app) | Container App configuration | <pre>object({<br>    min_replicas = number<br>    max_replicas = number<br><br>    scale_rules = list(object({<br>      name = string<br>      custom = object({<br>        metadata = map(string)<br>        type     = string<br>      })<br>    }))<br><br>    cpu    = number<br>    memory = string<br>  })</pre> | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for the container | `string` | `"latest"` | no |
| <a name="input_is_pnpg"></a> [is\_pnpg](#input\_is\_pnpg) | (Optional) True if you want to apply changes to PNPG environment | `bool` | `false` | no |
| <a name="input_private_dns_name"></a> [private\_dns\_name](#input\_private\_dns\_name) | Container Apps private DNS record | `string` | n/a | yes |
| <a name="input_secrets_names"></a> [secrets\_names](#input\_secrets\_names) | KeyVault secrets to get values from | `map(string)` | n/a | yes |
| <a name="input_suffix_increment"></a> [suffix\_increment](#input\_suffix\_increment) | Suffix increment Container App Environment name | `string` | `""` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_workload_profile_name"></a> [workload\_profile\_name](#input\_workload\_profile\_name) | Workload Profile name to use | `string` | `null` | no |

## Outputs

No outputs.
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->