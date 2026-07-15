# github_repository_environment

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |
| <a name="provider_github"></a> [github](#provider\_github) | 6.12.1 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [github_actions_environment_secret.env_secrets](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.kv_secrets](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_repository_environment.this](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/repository_environment) | resource |
| [github_repository_environment_deployment_policy.this](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/repository_environment_deployment_policy) | resource |
| [azurerm_key_vault_secret.kv_secrets](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [github_organization_teams.all](https://registry.terraform.io/providers/hashicorp/github/latest/docs/data-sources/organization_teams) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_branch_policy_enabled"></a> [branch\_policy\_enabled](#input\_branch\_policy\_enabled) | Whether branch policy is enabled for this environment | `bool` | n/a | yes |
| <a name="input_env"></a> [env](#input\_env) | Base environment name (e.g. prod, uat, dev) | `string` | n/a | yes |
| <a name="input_env_secrets"></a> [env\_secrets](#input\_env\_secrets) | Map of GitHub secret name to plaintext value | `map(string)` | `{}` | no |
| <a name="input_env_suffix"></a> [env\_suffix](#input\_env\_suffix) | Environment suffix: ci or cd | `string` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Azure Key Vault ID for reading secrets (required when kv\_secrets is non-empty) | `string` | `null` | no |
| <a name="input_kv_secrets"></a> [kv\_secrets](#input\_kv\_secrets) | Map of GitHub secret name to Key Vault secret name | `map(string)` | `{}` | no |
| <a name="input_repository"></a> [repository](#input\_repository) | GitHub repository name | `string` | n/a | yes |
| <a name="input_repository_environment"></a> [repository\_environment](#input\_repository\_environment) | GitHub repository environment configuration | <pre>object({<br/>    protected_branches     = bool<br/>    custom_branch_policies = bool<br/>    reviewers_teams        = list(string)<br/>    branch_pattern         = optional(string)<br/>  })</pre> | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
