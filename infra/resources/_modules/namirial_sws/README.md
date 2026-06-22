# namirial_sws

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azapi"></a> [azapi](#requirement\_azapi) | ~> 2.9.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_container_app.namirial_container_app](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app) | resource |
| [azurerm_container_app_environment_storage.namirial_sws_storage_env](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_storage) | resource |
| [azurerm_container_group.namirial_sws_cg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_group) | resource |
| [azurerm_key_vault_access_policy.keyvault_containerapp_access_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_private_dns_a_record.private_dns_record_a_azurecontainerapps_io](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/private_dns_a_record) | resource |
| [azurerm_storage_account.namirial_sws_storage_account](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_account) | resource |
| [azurerm_storage_share.namirial_sws_storage_share](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_share) | resource |
| [azurerm_subnet.namirial_sws_snet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_container_app_environment.container_app_environment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.hub_docker_pwd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.hub_docker_user](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.keyvault_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secrets.key_vault_secrets](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secrets) | data source |
| [azurerm_log_analytics_workspace.log_analytics](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/log_analytics_workspace) | data source |
| [azurerm_private_dns_zone.private_azurecontainerapps_io](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_resource_group.rg_contracts_storage](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_resource_group.rg_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_virtual_network.vnet_selc](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | n/a | <pre>list(object({<br/>    name  = string<br/>    value = string<br/>  }))</pre> | n/a | yes |
| <a name="input_cae_name"></a> [cae\_name](#input\_cae\_name) | Container App Environment name | `string` | `"cae-cp"` | no |
| <a name="input_cidr_subnet_namirial_sws"></a> [cidr\_subnet\_namirial\_sws](#input\_cidr\_subnet\_namirial\_sws) | Subnet address space. | `list(string)` | <pre>[<br/>  "10.1.154.0/29"<br/>]</pre> | no |
| <a name="input_container_app"></a> [container\_app](#input\_container\_app) | Container App configuration | <pre>object({<br/>    min_replicas = number<br/>    max_replicas = number<br/><br/>    scale_rules = list(object({<br/>      name = string<br/>      custom = object({<br/>        metadata = map(string)<br/>        type     = string<br/>      })<br/>    }))<br/><br/>    cpu    = number<br/>    memory = string<br/>  })</pre> | n/a | yes |
| <a name="input_container_config"></a> [container\_config](#input\_container\_config) | Container configuration | <pre>object({<br/>    cpu    = number<br/>    memory = number<br/>  })</pre> | n/a | yes |
| <a name="input_cross_tenant_replication_enabled"></a> [cross\_tenant\_replication\_enabled](#input\_cross\_tenant\_replication\_enabled) | Should cross Tenant replication be enabled? | `bool` | `false` | no |
| <a name="input_enable_ca_sws"></a> [enable\_ca\_sws](#input\_enable\_ca\_sws) | n/a | `bool` | `false` | no |
| <a name="input_enable_sws"></a> [enable\_sws](#input\_enable\_sws) | n/a | `bool` | `false` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_environment_variables"></a> [environment\_variables](#input\_environment\_variables) | environment variables for container | `map(string)` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_port"></a> [port](#input\_port) | Container binding port | `number` | `8080` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | Domain prefix | `string` | `"selc"` | no |
| <a name="input_secrets_names"></a> [secrets\_names](#input\_secrets\_names) | KeyVault secrets to get values from <env,secret-ref> | `map(string)` | `{}` | no |
| <a name="input_suffix_increment"></a> [suffix\_increment](#input\_suffix\_increment) | Suffix increment Container App Environment name | `string` | `""` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_workload_profile_name"></a> [workload\_profile\_name](#input\_workload\_profile\_name) | Workload Profile name to use | `string` | `"Consumption"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
