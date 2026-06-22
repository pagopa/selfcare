# github_managed_identity_cd

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
| <a name="module_identity_cd"></a> [identity\_cd](#module\_identity\_cd) | github.com/pagopa/terraform-azurerm-v4//github_federated_identity | v9.6.1 |
| <a name="module_identity_cd_fe"></a> [identity\_cd\_fe](#module\_identity\_cd\_fe) | github.com/pagopa/terraform-azurerm-v4//github_federated_identity | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.key_vault_access_policy_identity_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.key_vault_access_policy_identity_fe_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.key_vault_access_policy_pnpg_identity_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_access_policy.key_vault_access_policy_pnpg_identity_fe_cd](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_role_definition.container_apps_jobs_writer](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/role_definition) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app"></a> [app](#input\_app) | Application name (used in custom role names) | `string` | n/a | yes |
| <a name="input_cd_github_federations"></a> [cd\_github\_federations](#input\_cd\_github\_federations) | GitHub federations for CD main identity | <pre>list(object({<br/>    repository = string<br/>    subject    = string<br/>  }))</pre> | n/a | yes |
| <a name="input_cd_github_federations_fe"></a> [cd\_github\_federations\_fe](#input\_cd\_github\_federations\_fe) | GitHub federations for CD frontend identity | <pre>list(object({<br/>    repository = string<br/>    subject    = string<br/>  }))</pre> | n/a | yes |
| <a name="input_domain"></a> [domain](#input\_domain) | Domain name | `string` | n/a | yes |
| <a name="input_env"></a> [env](#input\_env) | Environment name (used in custom role names) | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | Short environment identifier | `string` | n/a | yes |
| <a name="input_environment_cd_roles"></a> [environment\_cd\_roles](#input\_environment\_cd\_roles) | RBAC roles for CD main identity | <pre>object({<br/>    subscription    = list(string)<br/>    resource_groups = map(list(string))<br/>  })</pre> | n/a | yes |
| <a name="input_environment_cd_roles_ms"></a> [environment\_cd\_roles\_ms](#input\_environment\_cd\_roles\_ms) | RBAC roles for CD microservices/frontend identity | <pre>object({<br/>    subscription    = list(string)<br/>    resource_groups = map(list(string))<br/>  })</pre> | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Azure Key Vault ID | `string` | n/a | yes |
| <a name="input_key_vault_pnpg_id"></a> [key\_vault\_pnpg\_id](#input\_key\_vault\_pnpg\_id) | Azure Key Vault PNPG ID | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | Project prefix | `string` | n/a | yes |
| <a name="input_subscription_id"></a> [subscription\_id](#input\_subscription\_id) | Azure subscription ID | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | Resource tags | `map(string)` | n/a | yes |
| <a name="input_tenant_id"></a> [tenant\_id](#input\_tenant\_id) | Azure tenant ID | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_identity_client_id"></a> [identity\_client\_id](#output\_identity\_client\_id) | n/a |
| <a name="output_identity_fe_client_id"></a> [identity\_fe\_client\_id](#output\_identity\_fe\_client\_id) | n/a |
| <a name="output_identity_principal_id"></a> [identity\_principal\_id](#output\_identity\_principal\_id) | n/a |
<!-- END_TF_DOCS -->
