# azure_devops_agent

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
| <a name="module_azdoa_li"></a> [azdoa\_li](#module\_azdoa\_li) | github.com/pagopa/terraform-azurerm-v4.git//azure_devops_agent | v9.6.1 |
| <a name="module_azdoa_li_infra"></a> [azdoa\_li\_infra](#module\_azdoa\_li\_infra) | github.com/pagopa/terraform-azurerm-v4.git//azure_devops_agent | v9.6.1 |
| <a name="module_azdoa_snet"></a> [azdoa\_snet](#module\_azdoa\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.azdevops_app_projects_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.azdevops_iac_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_resource_group.azdo_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_projects_principal_object_id"></a> [app\_projects\_principal\_object\_id](#input\_app\_projects\_principal\_object\_id) | Service principal for App Projects pipelines | `string` | n/a | yes |
| <a name="input_azdo_agent_vm_sku"></a> [azdo\_agent\_vm\_sku](#input\_azdo\_agent\_vm\_sku) | sku of the azdo agent vm | `string` | `"Standard_B1s"` | no |
| <a name="input_cidr_subnet_azdoa"></a> [cidr\_subnet\_azdoa](#input\_cidr\_subnet\_azdoa) | Azure DevOps agent network address space. | `list(string)` | n/a | yes |
| <a name="input_enable_app_projects_pipeline"></a> [enable\_app\_projects\_pipeline](#input\_enable\_app\_projects\_pipeline) | If true create the key vault policy to allow used by azure devops app projects pipelines. | `bool` | `false` | no |
| <a name="input_enable_azdoa"></a> [enable\_azdoa](#input\_enable\_azdoa) | Enable Azure DevOps agent. | `bool` | n/a | yes |
| <a name="input_enable_iac_pipeline"></a> [enable\_iac\_pipeline](#input\_enable\_iac\_pipeline) | If true create the key vault policy to allow used by azure devops iac pipelines. | `bool` | `false` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_iac_principal_object_id"></a> [iac\_principal\_object\_id](#input\_iac\_principal\_object\_id) | Service principal for IAC pipelines | `string` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Key Vault ID | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | Private endpoint network policies | `string` | `"Enabled"` | no |
| <a name="input_project"></a> [project](#input\_project) | n/a | `string` | `"selc"` | no |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | Resource group name for the VNet (for DNS zone) | `string` | n/a | yes |
| <a name="input_subscription_id"></a> [subscription\_id](#input\_subscription\_id) | Azure subscription ID | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | <pre>{<br/>  "CreatedBy": "Terraform"<br/>}</pre> | no |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Azure AD tenant ID for Front Door managed identity access to Key Vault | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | VNet name for the VNet (for DNS zone) | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
