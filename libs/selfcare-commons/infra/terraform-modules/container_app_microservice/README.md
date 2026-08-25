# SelfCare Container App microservice

This module deploys SelfCare microservices on a Container App. Then gives it access to the KeyVault instance to grab secrets.

Then, it updates the private DNS zone with a new A record pointing at the existing Container App Environment but with the Container App name as prefix.

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.6.0 |
| <a name="requirement_azapi"></a> [azapi](#requirement\_azapi) | ~> 1.9.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | <= 3.91.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azapi"></a> [azapi](#provider\_azapi) | 1.9.0 |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.91.0 |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [azurerm_container_app.container_app](https://registry.terraform.io/providers/azure/azapi/latest/docs/resources/resource) | resource |
| [azurerm_key_vault_access_policy.keyvault_containerapp_access_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_private_dns_a_record.private_dns_record_a_azurecontainerapps_io](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/private_dns_a_record) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_container_app_environment.container_app_environment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.keyvault_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secrets.key_vault_secrets](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secrets) | data source |
| [azurerm_private_dns_zone.private_azurecontainerapps_io](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_resource_group.resource_group_app](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |
| [azurerm_resource_group.rg_vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | n/a | <pre>list(object({<br/>    name  = string<br/>    value = string<br/>  }))</pre> | n/a | yes |
| <a name="input_container_app"></a> [container\_app](#input\_container\_app) | Container App configuration | <pre>object({<br/>    min_replicas = number<br/>    max_replicas = number<br/><br/>    scale_rules = list(object({<br/>      name = string<br/>      custom = object({<br/>        metadata = map(string)<br/>        type     = string<br/>      })<br/>    }))<br/><br/>    cpu    = number<br/>    memory = string<br/>  })</pre> | n/a | yes |
| <a name="input_container_app_environment_name"></a> [container\_app\_environment\_name](#input\_container\_app\_environment\_name) | Container app environment name to use | `string` | n/a | yes |
| <a name="input_container_app_name"></a> [container\_app\_name](#input\_container\_app\_name) | Container App name suffix | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Environment short name | `string` | n/a | yes |
| <a name="input_image_name"></a> [image\_name](#input\_image\_name) | Name of the image to use, hosted on GitHub container registry | `string` | n/a | yes |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | Image tag to use for the container | `string` | `"latest"` | no |
| <a name="input_is_pnpg"></a> [is\_pnpg](#input\_is\_pnpg) | (Optional) True if you want to apply changes to PNPG environment | `bool` | `false` | no |
| <a name="input_port"></a> [port](#input\_port) | Container binding port | `number` | `8080` | no |
| <a name="input_probes"></a> [probes](#input\_probes) | n/a | <pre>list(object({<br/>    type                = string<br/>    timeoutSeconds      = number<br/>    failureThreshold    = number<br/>    initialDelaySeconds = number<br/>    httpGet = object({<br/>      path   = string<br/>      scheme = string<br/>      port   = number<br/>    })<br/>  }))</pre> | <pre>[<br/>  {<br/>    "failureThreshold": 3,<br/>    "httpGet": {<br/>      "path": "actuator/health",<br/>      "port": 8080,<br/>      "scheme": "HTTP"<br/>    },<br/>    "initialDelaySeconds": 1,<br/>    "timeoutSeconds": 30,<br/>    "type": "Liveness"<br/>  },<br/>  {<br/>    "failureThreshold": 30,<br/>    "httpGet": {<br/>      "path": "actuator/health",<br/>      "port": 8080,<br/>      "scheme": "HTTP"<br/>    },<br/>    "initialDelaySeconds": 3,<br/>    "timeoutSeconds": 30,<br/>    "type": "Readiness"<br/>  },<br/>  {<br/>    "failureThreshold": 30,<br/>    "httpGet": {<br/>      "path": "actuator/health",<br/>      "port": 8080,<br/>      "scheme": "HTTP"<br/>    },<br/>    "initialDelaySeconds": 30,<br/>    "timeoutSeconds": 30,<br/>    "type": "Startup"<br/>  }<br/>]</pre> | no |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name) | Container app environment resource group name | `string` | n/a | yes |
| <a name="input_secrets_names"></a> [secrets\_names](#input\_secrets\_names) | KeyVault secrets to get values from <env,secret-ref> | `map(string)` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_workload_profile_name"></a> [workload\_profile\_name](#input\_workload\_profile\_name) | Workload Profile name to use | `string` | `"Consumption"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_container_app_environment_name"></a> [container\_app\_environment\_name](#output\_container\_app\_environment\_name) | n/a |
| <a name="output_container_app_name"></a> [container\_app\_name](#output\_container\_app\_name) | n/a |
| <a name="output_container_app_resource_group_name"></a> [container\_app\_resource\_group\_name](#output\_container\_app\_resource\_group\_name) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
