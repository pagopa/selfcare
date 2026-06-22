# functions

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 3.95 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.117.1 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_onboarding_fn_snet"></a> [onboarding\_fn\_snet](#module\_onboarding\_fn\_snet) | github.com/pagopa/terraform-azurerm-v3.git//subnet | v8.53.0 |
| <a name="module_selc_onboarding_fn"></a> [selc\_onboarding\_fn](#module\_selc\_onboarding\_fn) | github.com/pagopa/terraform-azurerm-v3.git//function_app | v8.53.0 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.fn_keyvault_access_policy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_secret.fn_primary_key](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_resource_group.fn_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_subnet_nat_gateway_association.fn_subnet_nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet_nat_gateway_association) | resource |
| [azurerm_key_vault_secret.appinsights_connection_string](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_nat_gateway.fn_nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/nat_gateway) | data source |
| [azurerm_resource_group.fn_nat_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/resource_group) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_always_on"></a> [always\_on](#input\_always\_on) | Always on for the function app | `bool` | `false` | no |
| <a name="input_app_settings"></a> [app\_settings](#input\_app\_settings) | Settings references to be set as app settings in the function app | `map(any)` | n/a | yes |
| <a name="input_application_insights_connection_string"></a> [application\_insights\_connection\_string](#input\_application\_insights\_connection\_string) | Application Insights connection string for linking diagnostics | `string` | `null` | no |
| <a name="input_application_insights_connection_string_secret_name"></a> [application\_insights\_connection\_string\_secret\_name](#input\_application\_insights\_connection\_string\_secret\_name) | Key Vault secret name for the Application Insights connection string | `string` | `"appinsights-connection-string"` | no |
| <a name="input_application_insights_key"></a> [application\_insights\_key](#input\_application\_insights\_key) | Application Insights instrumentation key | `string` | `null` | no |
| <a name="input_functions_name"></a> [functions\_name](#input\_functions\_name) | Name of the onboarding function app | `string` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | n/a | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_nat_gateway_name"></a> [nat\_gateway\_name](#input\_nat\_gateway\_name) | Name of NAT Gateway | `string` | n/a | yes |
| <a name="input_nat_resource_group_name"></a> [nat\_resource\_group\_name](#input\_nat\_resource\_group\_name) | Name of NAT Resource Group | `string` | n/a | yes |
| <a name="input_replication_type"></a> [replication\_type](#input\_replication\_type) | Storage account replication type | `string` | `"LRS"` | no |
| <a name="input_service_plan_sku"></a> [service\_plan\_sku](#input\_service\_plan\_sku) | SKU of the service plan for the function app | `string` | `"B2"` | no |
| <a name="input_service_plan_worker_count"></a> [service\_plan\_worker\_count](#input\_service\_plan\_worker\_count) | Worker count | `number` | `1` | no |
| <a name="input_subnet_cidr"></a> [subnet\_cidr](#input\_subnet\_cidr) | Network address space. | `list(string)` | `[]` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | n/a | yes |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Azure tenant ID | `string` | n/a | yes |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | Name of the Virtual Network | `string` | n/a | yes |
| <a name="input_vnet_resource_group_name"></a> [vnet\_resource\_group\_name](#input\_vnet\_resource\_group\_name) | Name of the Virtual Network Resource Group | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
