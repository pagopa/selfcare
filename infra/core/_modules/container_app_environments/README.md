# container_app_environments

<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [azurerm_container_app_environment.cae](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment) | resource |
| [azurerm_management_lock.identity_lock](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_management_lock.lock_cae](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/management_lock) | resource |
| [azurerm_user_assigned_identity.cae_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/user_assigned_identity) | resource |
| [azurerm_log_analytics_workspace.log_analytics_workspace](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/log_analytics_workspace) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_cae_name"></a> [cae\_name](#input\_cae\_name) | Name of Container App env | `string` | n/a | yes |
| <a name="input_enable_log"></a> [enable\_log](#input\_enable\_log) | Enable or disable logging | `bool` | `true` | no |
| <a name="input_infrastructure_resource_group_name"></a> [infrastructure\_resource\_group\_name](#input\_infrastructure\_resource\_group\_name) | Name of the platform-managed resource group created for the Managed Environment to host infrastructure resources. Changing this forces a new resource to be created. | `string` | `null` | no |
| <a name="input_location"></a> [location](#input\_location) | Azure region | `string` | n/a | yes |
| <a name="input_project"></a> [project](#input\_project) | SelfCare prefix and short environment | `string` | n/a | yes |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name) | Name of the resource group where resources will be created | `string` | n/a | yes |
| <a name="input_subnet_id"></a> [subnet\_id](#input\_subnet\_id) | Id of the subnet to use for Container App Environment | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | Resource tags | `map(any)` | n/a | yes |
| <a name="input_workload_profiles"></a> [workload\_profiles](#input\_workload\_profiles) | Workload profiles | <pre>list(object({<br/>    name                  = string<br/>    workload_profile_type = string<br/>    minimum_count         = number<br/>    maximum_count         = number<br/>  }))</pre> | <pre>[<br/>  {<br/>    "maximum_count": 1,<br/>    "minimum_count": 0,<br/>    "name": "Consumption",<br/>    "workload_profile_type": "Consumption"<br/>  }<br/>]</pre> | no |
| <a name="input_zone_redundant"></a> [zone\_redundant](#input\_zone\_redundant) | Enable or not the zone redundancy | `bool` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_container_app_environment"></a> [container\_app\_environment](#output\_container\_app\_environment) | n/a |
| <a name="output_user_assigned_identity"></a> [user\_assigned\_identity](#output\_user\_assigned\_identity) | Details about the user-assigned managed identity created to manage roles of the Container Apps of Selfcare Environment |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
