# uat-pnpg

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

No providers.

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_api_bff_dashboard_pnpg"></a> [apim\_api\_bff\_dashboard\_pnpg](#module\_apim\_api\_bff\_dashboard\_pnpg) | ../../_modules/apim_api | n/a |
| <a name="module_container_app_dashboard_bff_pnpg"></a> [container\_app\_dashboard\_bff\_pnpg](#module\_container\_app\_dashboard\_bff\_pnpg) | ../../_modules/container_app_microservice | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

No resources.

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | n/a | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
