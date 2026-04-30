# SelfCare Container App Job

This module deploys SelfCare jobs on a Container App Job. It gives the job access to the KeyVault instance to grab secrets.

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.6.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | > 4 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | > 4 |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [azurerm_container_app_job.container_app_job](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_job) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_container_app_environment.container_app_environment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.keyvault_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secrets.key_vault_secrets](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secrets) | data source |
| [azurerm_resource_group.resource_group_app](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_user_assigned_identity.cae_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | n/a | <pre>list(object({<br/>    name                  = string<br/>    value                 = optional(string, "")<br/>    key_vault_secret_name = optional(string)<br/>  }))</pre> | n/a | yes |
| <a name="input_container_app"></a> [container\_app](#input\_container\_app) | Container App Job configuration | <pre>object({<br/>    cpu    = number<br/>    memory = string<br/>  })</pre> | n/a | yes |
| <a name="input_container_app_environment_name"></a> [container\_app\_environment\_name](#input\_container\_app\_environment\_name) | Container app environment name to use | `string` | n/a | yes |
| <a name="input_container_app_name"></a> [container\_app\_name](#input\_container\_app\_name) | Container App Job name suffix | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_image_name"></a> [image\_name](#input\_image\_name) | Name of the image to use, hosted on GitHub container registry | `string` | n/a | yes |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for the container | `string` | `"latest"` | no |
| <a name="input_key_vault_name"></a> [key\_vault\_name](#input\_key\_vault\_name) | Key Vault name (for custom domain certificate) | `string` | n/a | yes |
| <a name="input_key_vault_resource_group_name"></a> [key\_vault\_resource\_group\_name](#input\_key\_vault\_resource\_group\_name) | Key Vault resource group name (for custom domain certificate) | `string` | n/a | yes |
| <a name="input_manual_trigger_config"></a> [manual\_trigger\_config](#input\_manual\_trigger\_config) | Manual trigger configuration for the container app job | <pre>list(object({<br/>    parallelism              = optional(number, 1)<br/>    replica_completion_count = optional(number, 1)<br/>  }))</pre> | `[]` | no |
| <a name="input_replica_retry_limit"></a> [replica\_retry\_limit](#input\_replica\_retry\_limit) | Maximum number of retries for a failed replica | `number` | `0` | no |
| <a name="input_replica_timeout_in_seconds"></a> [replica\_timeout\_in\_seconds](#input\_replica\_timeout\_in\_seconds) | Maximum number of seconds a replica is allowed to run | `number` | `28800` | no |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name) | Container app environment resource group name | `string` | n/a | yes |
| <a name="input_schedule_trigger_config"></a> [schedule\_trigger\_config](#input\_schedule\_trigger\_config) | Schedule trigger configuration for the container app job | <pre>list(object({<br/>    cron_expression          = string<br/>    parallelism              = optional(number, 1)<br/>    replica_completion_count = optional(number, 1)<br/>  }))</pre> | `[]` | no |
| <a name="input_secrets_names"></a> [secrets\_names](#input\_secrets\_names) | KeyVault secrets to get values from <env,secret-ref> | `map(string)` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_workload_profile_name"></a> [workload\_profile\_name](#input\_workload\_profile\_name) | Workload Profile name to use | `string` | `"Consumption"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_cae_identity_client_id"></a> [cae\_identity\_client\_id](#output\_cae\_identity\_client\_id) | n/a |
| <a name="output_cae_identity_id"></a> [cae\_identity\_id](#output\_cae\_identity\_id) | n/a |
| <a name="output_container_app_environment_name"></a> [container\_app\_environment\_name](#output\_container\_app\_environment\_name) | n/a |
| <a name="output_container_app_job_name"></a> [container\_app\_job\_name](#output\_container\_app\_job\_name) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
