# Repository Settings

Define settings of this GitHub repository.

## How to use

Make sure your PAT has access to this repository. Then, follow these steps:

- set the subscription: `az account set --subscription "PROD-SelfCare"`
- run `terraform init`
- run `terraform plan`
- run `terraform apply`

<!-- markdownlint-disable -->
<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_github"></a> [github](#requirement\_github) | ~> 6.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_github_repository"></a> [github\_repository](#module\_github\_repository) | pagopa-dx/github-environment-bootstrap/github | ~> 1.0 |

## Resources

No resources.

## Inputs

No inputs.

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_repository_id"></a> [repository\_id](#output\_repository\_id) | The ID of the GitHub repository |
| <a name="output_repository_name"></a> [repository\_name](#output\_repository\_name) | The name of the GitHub repository |
<!-- END_TF_DOCS -->