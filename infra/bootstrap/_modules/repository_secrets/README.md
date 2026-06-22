# repository_secrets

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_github"></a> [github](#provider\_github) | 6.12.1 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [github_actions_environment_secret.repo_fe_cd_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_fe_cd_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_fe_cd_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_fe_ci_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_fe_ci_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_fe_ci_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_cd_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_cd_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_cd_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_ci_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_ci_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_ms_ci_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_cd_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_cd_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_cd_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_ci_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_ci_secrets_subscription_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_environment_secret.repo_opex_ci_secrets_tenant_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_environment_secret) | resource |
| [github_actions_secret.repo_fe_cd_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_secret) | resource |
| [github_actions_secret.repo_ms_ci_secrets_client_id](https://registry.terraform.io/providers/hashicorp/github/latest/docs/resources/actions_secret) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_cd_identity_client_id"></a> [cd\_identity\_client\_id](#input\_cd\_identity\_client\_id) | Client ID of the Azure AD application for the backend CD pipeline | `string` | n/a | yes |
| <a name="input_ci_identity_client_id"></a> [ci\_identity\_client\_id](#input\_ci\_identity\_client\_id) | Client ID of the Azure AD application for the backend CI pipeline | `string` | n/a | yes |
| <a name="input_fe_cd_identity_client_id"></a> [fe\_cd\_identity\_client\_id](#input\_fe\_cd\_identity\_client\_id) | Client ID of the Azure AD application for the frontend CD pipeline | `string` | n/a | yes |
| <a name="input_fe_ci_identity_client_id"></a> [fe\_ci\_identity\_client\_id](#input\_fe\_ci\_identity\_client\_id) | Client ID of the Azure AD application for the frontend CI pipeline | `string` | n/a | yes |
| <a name="input_gh_pat_variable"></a> [gh\_pat\_variable](#input\_gh\_pat\_variable) | Github pat key | `string` | n/a | yes |
| <a name="input_github_federations"></a> [github\_federations](#input\_github\_federations) | Micro-services mapping for GitHub Workload Identity Federation | `map(string)` | `{}` | no |
| <a name="input_github_federations_fe"></a> [github\_federations\_fe](#input\_github\_federations\_fe) | Micro-frontend mapping for GitHub Workload Identity Federation | `map(string)` | `{}` | no |
| <a name="input_opex"></a> [opex](#input\_opex) | Opex Dashboards configuration enabled | `bool` | `false` | no |
| <a name="input_opex_cd_identity_client_id"></a> [opex\_cd\_identity\_client\_id](#input\_opex\_cd\_identity\_client\_id) | Client ID of the Azure AD application for the OPEX CD pipeline | `string` | `null` | no |
| <a name="input_opex_ci_identity_client_id"></a> [opex\_ci\_identity\_client\_id](#input\_opex\_ci\_identity\_client\_id) | Client ID of the Azure AD application for the OPEX CI pipeline | `string` | `null` | no |
| <a name="input_subscription_id"></a> [subscription\_id](#input\_subscription\_id) | Azure subscription ID | `string` | n/a | yes |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Azure tenant ID | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
