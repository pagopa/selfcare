# Container App

Deploy container on a Container App instance.

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.9.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.27.0 |

## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_apim_api_bff_proxy"></a> [apim\_api\_bff\_proxy](#module\_apim\_api\_bff\_proxy) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v7.26.5 |
| <a name="module_container_app_party_reg_proxy"></a> [container\_app\_party\_reg\_proxy](#module\_container\_app\_party\_reg\_proxy) | github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice | v1.1.1 |

## Resources

| Name | Type |
|------|------|
| [azurerm_api_management_api_version_set.apim_api_bff_proxy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_user_assigned_identity.cae_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_api_dns_zone_prefix"></a> [api\_dns\_zone\_prefix](#input\_api\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"api.selfcare"` | no |
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | n/a | <pre>list(object({<br/>    name  = string<br/>    value = string<br/>  }))</pre> | n/a | yes |
| <a name="input_cae_name"></a> [cae\_name](#input\_cae\_name) | Container App Environment name | `string` | `"cae-cp"` | no |
| <a name="input_container_app"></a> [container\_app](#input\_container\_app) | Container App configuration | <pre>object({<br/>    min_replicas = number<br/>    max_replicas = number<br/><br/>    scale_rules = list(object({<br/>      name = string<br/>      custom = object({<br/>        metadata = map(string)<br/>        type     = string<br/>      })<br/>    }))<br/><br/>    cpu    = number<br/>    memory = string<br/>  })</pre> | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for the container | `string` | `"latest"` | no |
| <a name="input_is_pnpg"></a> [is\_pnpg](#input\_is\_pnpg) | (Optional) True if you want to apply changes to PNPG environment | `bool` | `false` | no |
| <a name="input_private_dns_name"></a> [private\_dns\_name](#input\_private\_dns\_name) | Container Apps private DNS record | `string` | `"selc-d-party-reg-proxy-ca.gentleflower-c63e62fe.westeurope.azurecontainerapps.io"` | no |
| <a name="input_secrets_names"></a> [secrets\_names](#input\_secrets\_names) | KeyVault secrets to get values from | `map(string)` | n/a | yes |
| <a name="input_suffix_increment"></a> [suffix\_increment](#input\_suffix\_increment) | Suffix increment Container App Environment name | `string` | `""` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_workload_profile_name"></a> [workload\_profile\_name](#input\_workload\_profile\_name) | Workload Profile name to use | `string` | `null` | no |

## Outputs

No outputs.
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->