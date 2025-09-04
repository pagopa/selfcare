# Repository Settings

Define settings of this GitHub repository.

## How to use

Make sure your PAT has access to this repository. Then, follow these steps:

- set the subscription: `az account set --subscription "PROD-SelfCare"`
- run `terraform init`
- run `terraform plan`
- run `terraform apply`

<!-- markdownlint-disable -->
<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | <= 3.112.0 |
| <a name="requirement_github"></a> [github](#requirement\_github) | 5.45.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 3.112.0 |
| <a name="provider_github"></a> [github](#provider\_github) | 5.45.0 |

## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_repository"></a> [repository](#module\_repository) | github.com/pagopa/selfcare-commons//infra/terraform-modules/github_repository_settings | main |

## Resources

| Name | Type |
|------|------|
| [github_repository.this](https://registry.terraform.io/providers/integrations/github/5.45.0/docs/resources/repository) | resource |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_subscription.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subscription) | data source |

## Inputs

No inputs.

## Outputs

No outputs.
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->