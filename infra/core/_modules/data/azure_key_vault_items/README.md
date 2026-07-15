# azure_key_vault_items

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_certificate.api_pnpg_selfcare_certificate](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_certificate) | data source |
| [azurerm_key_vault_certificate.app_gw_platform](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_certificate) | data source |
| [azurerm_key_vault_secret.apim_publisher_email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.sec_storage_id](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.sec_workspace_id](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.selc_documents_storage_connection_string](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_app_gateway_api_certificate_name"></a> [app\_gateway\_api\_certificate\_name](#input\_app\_gateway\_api\_certificate\_name) | Application gateway: api certificate name on Key Vault | `string` | n/a | yes |
| <a name="input_app_gateway_api_pnpg_certificate_name"></a> [app\_gateway\_api\_pnpg\_certificate\_name](#input\_app\_gateway\_api\_pnpg\_certificate\_name) | Application gateway: api-pnpg certificate name on Key Vault | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Key Vault ID | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_api_pnpg_cert_secret_id"></a> [api\_pnpg\_cert\_secret\_id](#output\_api\_pnpg\_cert\_secret\_id) | n/a |
| <a name="output_api_pnpg_cert_version"></a> [api\_pnpg\_cert\_version](#output\_api\_pnpg\_cert\_version) | n/a |
| <a name="output_api_pnpg_selfcare_certificate_secret_id"></a> [api\_pnpg\_selfcare\_certificate\_secret\_id](#output\_api\_pnpg\_selfcare\_certificate\_secret\_id) | n/a |
| <a name="output_apim_publisher_email"></a> [apim\_publisher\_email](#output\_apim\_publisher\_email) | n/a |
| <a name="output_app_gw_platform_cert_secret_id"></a> [app\_gw\_platform\_cert\_secret\_id](#output\_app\_gw\_platform\_cert\_secret\_id) | Certificate outputs for appgateway |
| <a name="output_app_gw_platform_cert_version"></a> [app\_gw\_platform\_cert\_version](#output\_app\_gw\_platform\_cert\_version) | n/a |
| <a name="output_app_gw_platform_certificate_secret_id"></a> [app\_gw\_platform\_certificate\_secret\_id](#output\_app\_gw\_platform\_certificate\_secret\_id) | n/a |
| <a name="output_sec_storage_id"></a> [sec\_storage\_id](#output\_sec\_storage\_id) | n/a |
| <a name="output_sec_workspace_id"></a> [sec\_workspace\_id](#output\_sec\_workspace\_id) | n/a |
<!-- END_TF_DOCS -->
