# identity

<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.64 |
| <a name="requirement_github"></a> [github](#requirement\_github) | ~> 6.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.68.0 |
| <a name="provider_github"></a> [github](#provider\_github) | 6.11.1 |

## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_github_environment_cd"></a> [github\_environment\_cd](#module\_github\_environment\_cd) | ../_modules/github_repository_environment | n/a |
| <a name="module_github_environment_ci"></a> [github\_environment\_ci](#module\_github\_environment\_ci) | ../_modules/github_repository_environment | n/a |
| <a name="module_github_runner"></a> [github\_runner](#module\_github\_runner) | ../_modules/github_runner | n/a |
| <a name="module_github_secrets"></a> [github\_secrets](#module\_github\_secrets) | ../_modules/repository_secrets | n/a |
| <a name="module_identity_cd"></a> [identity\_cd](#module\_identity\_cd) | ../_modules/github_managed_identity_cd | n/a |
| <a name="module_identity_ci"></a> [identity\_ci](#module\_identity\_ci) | ../_modules/github_managed_identity_ci | n/a |
| <a name="module_keyvault"></a> [keyvault](#module\_keyvault) | ../_modules/keyvault | n/a |
| <a name="module_tfstate_access"></a> [tfstate\_access](#module\_tfstate\_access) | ../_modules/tfstate_access | n/a |

## Resources

| Name | Type |
|------|------|
| [azurerm_resource_group.identity_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [github_branch_default.default_main](https://registry.terraform.io/providers/integrations/github/latest/docs/resources/branch_default) | resource |
| [github_branch_protection_v3.protection_main](https://registry.terraform.io/providers/integrations/github/latest/docs/resources/branch_protection_v3) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault.key_vault_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.github_path_token](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_subscription.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subscription) | data source |

## Inputs

No inputs.

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_subscription_id"></a> [subscription\_id](#output\_subscription\_id) | n/a |
| <a name="output_tenant_id"></a> [tenant\_id](#output\_tenant\_id) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
