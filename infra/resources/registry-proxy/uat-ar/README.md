# uat-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.10.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.67.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_ai_search_ipa"></a> [ai\_search\_ipa](#module\_ai\_search\_ipa) | ../../_modules/ai_search_ipa | n/a |
| <a name="module_ai_search_onboarding"></a> [ai\_search\_onboarding](#module\_ai\_search\_onboarding) | ../../_modules/ai_search_onboarding | n/a |
| <a name="module_apim_api_registry_proxy"></a> [apim\_api\_registry\_proxy](#module\_apim\_api\_registry\_proxy) | ../../_modules/apim_api | n/a |
| <a name="module_container_app_registry_proxy_ms"></a> [container\_app\_registry\_proxy\_ms](#module\_container\_app\_registry\_proxy\_ms) | ../../_modules/container_app_microservice | n/a |
| <a name="module_dapr"></a> [dapr](#module\_dapr) | ../../_modules/dapr | n/a |
| <a name="module_local"></a> [local](#module\_local) | ../../_modules/local-env | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_container_app_environment_dapr_component.blob_state](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app_environment_dapr_component) | resource |
| [azurerm_storage_container.visura](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/storage_container) | resource |
| [azurerm_container_app.ca](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app) | data source |
| [azurerm_container_app_environment.cae](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_search_service.srch_service](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/search_service) | data source |
| [azurerm_storage_account.existing_logs_storage](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_image_tag"></a> [image\_tag](#input\_image\_tag) | n/a | `string` | `"latest"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
