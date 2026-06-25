# apim_external_api

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | > 4.0.0 |
| <a name="requirement_pkcs12"></a> [pkcs12](#requirement\_pkcs12) | 0.2.5 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.72.0 |
| <a name="provider_null"></a> [null](#provider\_null) | 3.3.0 |
| <a name="provider_pkcs12"></a> [pkcs12](#provider\_pkcs12) | 0.2.5 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_apim_billing_portal_v1"></a> [apim\_billing\_portal\_v1](#module\_apim\_billing\_portal\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_external_api_contract_public_v1"></a> [apim\_external\_api\_contract\_public\_v1](#module\_apim\_external\_api\_contract\_public\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_external_api_contract_v1"></a> [apim\_external\_api\_contract\_v1](#module\_apim\_external\_api\_contract\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_external_api_ms_v2"></a> [apim\_external\_api\_ms\_v2](#module\_apim\_external\_api\_ms\_v2) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_external_api_onboarding_auto_v1"></a> [apim\_external\_api\_onboarding\_auto\_v1](#module\_apim\_external\_api\_onboarding\_auto\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_external_api_onboarding_io_v1"></a> [apim\_external\_api\_onboarding\_io\_v1](#module\_apim\_external\_api\_onboarding\_io\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_internal_api_ms_v1"></a> [apim\_internal\_api\_ms\_v1](#module\_apim\_internal\_api\_ms\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_internal_user_api_ms_v1"></a> [apim\_internal\_user\_api\_ms\_v1](#module\_apim\_internal\_user\_api\_ms\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_notification_event_api_v1"></a> [apim\_notification\_event\_api\_v1](#module\_apim\_notification\_event\_api\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_pdnd_infocamere_api_ms_v1"></a> [apim\_pdnd\_infocamere\_api\_ms\_v1](#module\_apim\_pdnd\_infocamere\_api\_ms\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_pnpg"></a> [apim\_pnpg](#module\_apim\_pnpg) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_pnpg_external_api_data_vault_v1"></a> [apim\_pnpg\_external\_api\_data\_vault\_v1](#module\_apim\_pnpg\_external\_api\_data\_vault\_v1) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_pnpg_external_api_ms_v2"></a> [apim\_pnpg\_external\_api\_ms\_v2](#module\_apim\_pnpg\_external\_api\_ms\_v2) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_pnpg_internal_api"></a> [apim\_pnpg\_internal\_api](#module\_apim\_pnpg\_internal\_api) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_pnpg_product_pn_pg"></a> [apim\_pnpg\_product\_pn\_pg](#module\_apim\_pnpg\_product\_pn\_pg) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_pnpg_support_service_v2"></a> [apim\_pnpg\_support\_service\_v2](#module\_apim\_pnpg\_support\_service\_v2) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_apim_product_fd"></a> [apim\_product\_fd](#module\_apim\_product\_fd) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_fd_garantito"></a> [apim\_product\_fd\_garantito](#module\_apim\_product\_fd\_garantito) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_idpay"></a> [apim\_product\_idpay](#module\_apim\_product\_idpay) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_idpay_gi"></a> [apim\_product\_idpay\_gi](#module\_apim\_product\_idpay\_gi) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_idpay_merchant"></a> [apim\_product\_idpay\_merchant](#module\_apim\_product\_idpay\_merchant) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_interop"></a> [apim\_product\_interop](#module\_apim\_product\_interop) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_interop_atst"></a> [apim\_product\_interop\_atst](#module\_apim\_product\_interop\_atst) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_interop_coll"></a> [apim\_product\_interop\_coll](#module\_apim\_product\_interop\_coll) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_io"></a> [apim\_product\_io](#module\_apim\_product\_io) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_io_premium"></a> [apim\_product\_io\_premium](#module\_apim\_product\_io\_premium) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_io_sign"></a> [apim\_product\_io\_sign](#module\_apim\_product\_io\_sign) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pagopa"></a> [apim\_product\_pagopa](#module\_apim\_product\_pagopa) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn"></a> [apim\_product\_pn](#module\_apim\_product\_pn) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_cert"></a> [apim\_product\_pn\_cert](#module\_apim\_product\_pn\_cert) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_coll"></a> [apim\_product\_pn\_coll](#module\_apim\_product\_pn\_coll) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_dev"></a> [apim\_product\_pn\_dev](#module\_apim\_product\_pn\_dev) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_hotfix"></a> [apim\_product\_pn\_hotfix](#module\_apim\_product\_pn\_hotfix) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_prod"></a> [apim\_product\_pn\_prod](#module\_apim\_product\_pn\_prod) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_svil"></a> [apim\_product\_pn\_svil](#module\_apim\_product\_pn\_svil) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_test"></a> [apim\_product\_pn\_test](#module\_apim\_product\_pn\_test) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pn_uat"></a> [apim\_product\_pn\_uat](#module\_apim\_product\_pn\_uat) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_dev"></a> [apim\_product\_pnpg\_dev](#module\_apim\_product\_pnpg\_dev) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_hotfix"></a> [apim\_product\_pnpg\_hotfix](#module\_apim\_product\_pnpg\_hotfix) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_test"></a> [apim\_product\_pnpg\_test](#module\_apim\_product\_pnpg\_test) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_uat"></a> [apim\_product\_pnpg\_uat](#module\_apim\_product\_pnpg\_uat) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_uat_cert"></a> [apim\_product\_pnpg\_uat\_cert](#module\_apim\_product\_pnpg\_uat\_cert) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_uat_coll"></a> [apim\_product\_pnpg\_uat\_coll](#module\_apim\_product\_pnpg\_uat\_coll) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_pnpg_uat_svil"></a> [apim\_product\_pnpg\_uat\_svil](#module\_apim\_product\_pnpg\_uat\_svil) | git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_registro_beni"></a> [apim\_product\_registro\_beni](#module\_apim\_product\_registro\_beni) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_support_io"></a> [apim\_product\_support\_io](#module\_apim\_product\_support\_io) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_test_io"></a> [apim\_product\_test\_io](#module\_apim\_product\_test\_io) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_product_test_io_premium"></a> [apim\_product\_test\_io\_premium](#module\_apim\_product\_test\_io\_premium) | github.com/pagopa/terraform-azurerm-v4.git//api_management_product | v10.9.0 |
| <a name="module_apim_selfcare_support_service_v1"></a> [apim\_selfcare\_support\_service\_v1](#module\_apim\_selfcare\_support\_service\_v1) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |
| <a name="module_monitor"></a> [monitor](#module\_monitor) | github.com/pagopa/terraform-azurerm-v4.git//api_management_api | v10.9.0 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_api_management_api_operation.check_recipient_code](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_operation) | resource |
| [azurerm_api_management_api_version_set.apim_billing_portal](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_contract](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_contracts_public](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_data_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_ms](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_onboarding_auto](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_onboarding_io](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_external_api_v2_for_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_internal_api_for_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_internal_api_ms](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_internal_user_api_ms](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_notification_event_api](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_pdnd_infocamere_api_ms](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_pnpg_support_service](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_api_version_set.apim_selfcare_support_service](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_api_version_set) | resource |
| [azurerm_api_management_certificate.jwt_certificate](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_certificate) | resource |
| [azurerm_api_management_certificate.jwt_certificate_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/api_management_certificate) | resource |
| [null_resource.upload_billing_developer_index_v1](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_developer_index_v2](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_internal_developer_index_v1](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_support_developer_index_v1](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.upload_support_pnpg_developer_index_v1](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [pkcs12_from_pem.jwt_pkcs12](https://registry.terraform.io/providers/chilicat/pkcs12/0.2.5/docs/resources/from_pem) | resource |
| [pkcs12_from_pem.jwt_pkcs12_pnpg](https://registry.terraform.io/providers/chilicat/pkcs12/0.2.5/docs/resources/from_pem) | resource |
| [azurerm_api_management.apim](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/api_management) | data source |
| [azurerm_application_insights.ai](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/application_insights) | data source |
| [azurerm_client_config.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/client_config) | data source |
| [azurerm_key_vault.key_vault](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault.key_vault_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault) | data source |
| [azurerm_key_vault_certificate.app_gw_platform](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_certificate) | data source |
| [azurerm_key_vault_secret.apim_backend_access_token](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.apim_publisher_email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.external-oauth2-issuer](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.fn-onboarding-primary-key](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_certificate_data_pem](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_certificate_data_pem_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_kid](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_kid_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_private_key_pem](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.jwt_private_key_pem_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.web_storage_url](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_storage_account.checkout](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |
| [azurerm_storage_account.checkout_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/storage_account) | data source |
| [azurerm_subscription.current](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subscription) | data source |
| [azurerm_virtual_network.vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_apim_publisher_name"></a> [apim\_publisher\_name](#input\_apim\_publisher\_name) | apim | `string` | n/a | yes |
| <a name="input_apim_sku"></a> [apim\_sku](#input\_apim\_sku) | n/a | `string` | n/a | yes |
| <a name="input_app_gateway_api_certificate_name"></a> [app\_gateway\_api\_certificate\_name](#input\_app\_gateway\_api\_certificate\_name) | Application gateway: api certificate name on Key Vault | `string` | n/a | yes |
| <a name="input_ca_pnpg_suffix_dns_private_name"></a> [ca\_pnpg\_suffix\_dns\_private\_name](#input\_ca\_pnpg\_suffix\_dns\_private\_name) | CA PNPG suffix private DNS record | `string` | n/a | yes |
| <a name="input_ca_suffix_dns_private_name"></a> [ca\_suffix\_dns\_private\_name](#input\_ca\_suffix\_dns\_private\_name) | CA suffix private DNS record | `string` | n/a | yes |
| <a name="input_cidr_subnet_apim"></a> [cidr\_subnet\_apim](#input\_cidr\_subnet\_apim) | Address prefixes subnet api management. | `list(string)` | `null` | no |
| <a name="input_developer_path"></a> [developer\_path](#input\_developer\_path) | Path where is located developer index.html file | `string` | n/a | yes |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_domain"></a> [domain](#input\_domain) | n/a | `string` | `"pnpg"` | no |
| <a name="input_env"></a> [env](#input\_env) | env directory name | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_location_short"></a> [location\_short](#input\_location\_short) | n/a | `string` | `"weu"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_private_dns_name"></a> [private\_dns\_name](#input\_private\_dns\_name) | AKS private DNS record | `string` | n/a | yes |
| <a name="input_private_onboarding_dns_name"></a> [private\_onboarding\_dns\_name](#input\_private\_onboarding\_dns\_name) | AKS private onboarding DNS record | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | <pre>{<br/>  "CreatedBy": "Terraform"<br/>}</pre> | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
