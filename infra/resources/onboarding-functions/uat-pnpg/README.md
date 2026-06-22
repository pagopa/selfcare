# uat-pnpg

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.6.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 3.95 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.117.1 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |
| <a name="module_onboarding_functions"></a> [onboarding\_functions](#module\_onboarding\_functions) | ../../_modules/functions | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_nat_gateway_public_ip_association.functions_pip_nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/nat_gateway_public_ip_association) | resource |
| [azurerm_nat_gateway.onboarding_functions_nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/nat_gateway) | data source |
| [azurerm_public_ip.pip_outbound](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/public_ip) | data source |

## Inputs

No inputs.

## Outputs

No outputs.
<!-- END_TF_DOCS -->
