# dev-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azuread"></a> [azuread](#requirement\_azuread) | ~> 3.8 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.64 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.74.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_keyvault_permissions"></a> [keyvault\_permissions](#module\_keyvault\_permissions) | ../../_modules/keyvault_permissions | n/a |
| <a name="module_keyvault_pnpg_permissions"></a> [keyvault\_pnpg\_permissions](#module\_keyvault\_pnpg\_permissions) | ../../_modules/keyvault_permissions | n/a |
| <a name="module_tfstate_access"></a> [tfstate\_access](#module\_tfstate\_access) | ../../_modules/tfstate_access | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault.key_vault_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |

## Inputs

No inputs.

## Outputs

No outputs.
<!-- END_TF_DOCS -->
