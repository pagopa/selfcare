# dev-ar

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >=1.10.0 |
| <a name="requirement_azuread"></a> [azuread](#requirement\_azuread) | >= 3.8.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | ~> 4.0 |
| <a name="requirement_dx"></a> [dx](#requirement\_dx) | ~> 0.0 |
| <a name="requirement_pkcs12"></a> [pkcs12](#requirement\_pkcs12) | 0.2.5 |
| <a name="requirement_random"></a> [random](#requirement\_random) | >= 3.0.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |
| <a name="provider_random"></a> [random](#provider\_random) | 3.8.1 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_ai_search"></a> [ai\_search](#module\_ai\_search) | ../_modules/ai_search | n/a |
| <a name="module_apim"></a> [apim](#module\_apim) | ../_modules/apim | n/a |
| <a name="module_appgateway"></a> [appgateway](#module\_appgateway) | ../_modules/appgateway | n/a |
| <a name="module_assets"></a> [assets](#module\_assets) | ../_modules/assets | n/a |
| <a name="module_azure_devops_agent"></a> [azure\_devops\_agent](#module\_azure\_devops\_agent) | ../_modules/azure_devops_agent | n/a |
| <a name="module_azure_key_vault_items"></a> [azure\_key\_vault\_items](#module\_azure\_key\_vault\_items) | ../_modules/data/azure_key_vault_items | n/a |
| <a name="module_cdn"></a> [cdn](#module\_cdn) | ../_modules/cdn | n/a |
| <a name="module_container_app_environments"></a> [container\_app\_environments](#module\_container\_app\_environments) | ../_modules/container_app_environments | n/a |
| <a name="module_contracts_storage"></a> [contracts\_storage](#module\_contracts\_storage) | ../_modules/storage_account_template | n/a |
| <a name="module_cosmos_db"></a> [cosmos\_db](#module\_cosmos\_db) | ../_modules/cosmos_db | n/a |
| <a name="module_default_roleassignment"></a> [default\_roleassignment](#module\_default\_roleassignment) | ../_modules/roles | n/a |
| <a name="module_dns_private"></a> [dns\_private](#module\_dns\_private) | ../_modules/dns_private | n/a |
| <a name="module_dns_public"></a> [dns\_public](#module\_dns\_public) | ../_modules/dns_public | n/a |
| <a name="module_events"></a> [events](#module\_events) | ../_modules/events | n/a |
| <a name="module_key_vault"></a> [key\_vault](#module\_key\_vault) | ../_modules/key_vault | n/a |
| <a name="module_log_analytics"></a> [log\_analytics](#module\_log\_analytics) | ../_modules/log_analytics | n/a |
| <a name="module_logs_storage"></a> [logs\_storage](#module\_logs\_storage) | ../_modules/storage_account_template | n/a |
| <a name="module_monitor"></a> [monitor](#module\_monitor) | ../_modules/monitor | n/a |
| <a name="module_nat"></a> [nat](#module\_nat) | ../_modules/nat | n/a |
| <a name="module_network"></a> [network](#module\_network) | ../_modules/network | n/a |
| <a name="module_networking"></a> [networking](#module\_networking) | ../_modules/networking | n/a |
| <a name="module_one_trust"></a> [one\_trust](#module\_one\_trust) | ../_modules/one_trust | n/a |
| <a name="module_redis"></a> [redis](#module\_redis) | ../_modules/redis | n/a |
| <a name="module_resources"></a> [resources](#module\_resources) | ../_modules/resources | n/a |
| <a name="module_spid_logs_encryption_keys"></a> [spid\_logs\_encryption\_keys](#module\_spid\_logs\_encryption\_keys) | ../_modules/spid_logs_encryption_keys | n/a |
| <a name="module_storage"></a> [storage](#module\_storage) | ../_modules/storage | n/a |
| <a name="module_storage_documents"></a> [storage\_documents](#module\_storage\_documents) | ../_modules/storage_accounts | n/a |
| <a name="module_upload_file_logo"></a> [upload\_file\_logo](#module\_upload\_file\_logo) | ../_modules/upload_file | n/a |
| <a name="module_vpn"></a> [vpn](#module\_vpn) | ../_modules/vpn | n/a |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_access_policy.container_app_environment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_access_policy) | resource |
| [azurerm_key_vault_secret.onboarding_encryption_iv_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.onboarding_encryption_key_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.selfcare_encryption_iv_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_key_vault_secret.selfcare_encryption_key_secret](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_portal_dashboard.monitoring_document_ms](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/portal_dashboard) | resource |
| [azurerm_portal_dashboard.monitoring_onboarding_event](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/portal_dashboard) | resource |
| [azurerm_portal_dashboard.overview](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/portal_dashboard) | resource |
| [azurerm_resource_group.documents_sa_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_resource_group.selc_cae_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |
| [azurerm_user_assigned_identity.documents_identity](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/user_assigned_identity) | resource |
| [random_password.encryption_iv](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/password) | resource |
| [random_password.encryption_key](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/password) | resource |
| [azurerm_key_vault_secret.selc_documents_storage_connection_string](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |

## Inputs

No inputs.

## Outputs

No outputs.
<!-- END_TF_DOCS -->
