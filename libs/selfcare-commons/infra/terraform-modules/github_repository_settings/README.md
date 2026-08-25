# SelfCare GitHub Repository Settings

This module contains some common settings to apply to SelfCare repositories.
Module is based on GitHub flow and contains:

- branch protection rules for both `main` and `releases/**` branches
- six GitHub environments (dev, uat and prod for both ci and cd cases), with variables, secrets and deployment permissions

Lot of things are hardcoded. If you like this module and use it in your project, make sure to change values in `locals.tf`.

## How to use

A simple line with your repository name is required:

```ts
module "repo_settings" {
  source = "github.com/pagopa/selfcare-commons//terraform/azure_github_federation"

  github = {
    repository = "selfcare-ms-user-group"
  }
}
```

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.6.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | >= 3.90.0 |
| <a name="requirement_github"></a> [github](#requirement\_github) | 5.45.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.90.0 |
| <a name="provider_azurerm.dev"></a> [azurerm.dev](#provider\_azurerm.dev) | 3.90.0 |
| <a name="provider_azurerm.prod"></a> [azurerm.prod](#provider\_azurerm.prod) | 3.90.0 |
| <a name="provider_azurerm.uat"></a> [azurerm.uat](#provider\_azurerm.uat) | 3.90.0 |
| <a name="provider_github"></a> [github](#provider\_github) | 5.45.0 |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [github_actions_environment_secret.env_dev_cd_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.env_dev_ci_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.env_prod_cd_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.env_prod_ci_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.env_uat_cd_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.env_uat_ci_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_variable.env_dev_cd_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_environment_variable.env_dev_ci_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_environment_variable.env_prod_cd_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_environment_variable.env_prod_ci_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_environment_variable.env_uat_cd_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_environment_variable.env_uat_ci_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_environment_variable) | resource |
| [github_actions_secret.repo_secrets](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_secret) | resource |
| [github_actions_variable.repo_variables](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/actions_variable) | resource |
| [github_branch_default.default_main](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/branch_default) | resource |
| [github_branch_protection.protection_main](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/branch_protection) | resource |
| [github_branch_protection.protection_releases](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/branch_protection) | resource |
| [github_repository_environment.github_repository_environment_dev_cd](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [github_repository_environment.github_repository_environment_dev_ci](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [github_repository_environment.github_repository_environment_prod_cd](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [github_repository_environment.github_repository_environment_prod_ci](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [github_repository_environment.github_repository_environment_uat_cd](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [github_repository_environment.github_repository_environment_uat_ci](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository_environment) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_key_vault.key_vault_dev](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_secret.key_github_path_token](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.key_vault_sonar](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_user_assigned_identity.identity_dev_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_dev_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_dev_fe_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_dev_fe_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_prod_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_prod_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_prod_fe_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_prod_fe_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_uat_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_uat_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_uat_fe_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [azurerm_user_assigned_identity.identity_uat_fe_ci](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/user_assigned_identity) | data source |
| [github_organization_teams.all](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/data-sources/organization_teams) | data source |
| [github_team.team_admins](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/data-sources/team) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_github"></a> [github](#input\_github) | n/a | <pre>object({<br/>    repository = string<br/>  })</pre> | n/a | yes |
| <a name="input_identity_component"></a> [identity\_component](#input\_identity\_component) | n/a | `string` | `"ms"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_managed_identity_id_dev_cd"></a> [managed\_identity\_id\_dev\_cd](#output\_managed\_identity\_id\_dev\_cd) | n/a |
| <a name="output_managed_identity_id_dev_ci"></a> [managed\_identity\_id\_dev\_ci](#output\_managed\_identity\_id\_dev\_ci) | n/a |
| <a name="output_managed_identity_id_prod_cd"></a> [managed\_identity\_id\_prod\_cd](#output\_managed\_identity\_id\_prod\_cd) | n/a |
| <a name="output_managed_identity_id_prod_ci"></a> [managed\_identity\_id\_prod\_ci](#output\_managed\_identity\_id\_prod\_ci) | n/a |
| <a name="output_managed_identity_id_uat_cd"></a> [managed\_identity\_id\_uat\_cd](#output\_managed\_identity\_id\_uat\_cd) | n/a |
| <a name="output_managed_identity_id_uat_ci"></a> [managed\_identity\_id\_uat\_ci](#output\_managed\_identity\_id\_uat\_ci) | n/a |
| <a name="output_tenant_id"></a> [tenant\_id](#output\_tenant\_id) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
