# monitor

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
| <a name="module_web_test_api"></a> [web\_test\_api](#module\_web\_test\_api) | github.com/pagopa/terraform-azurerm-v4.git//application_insights_web_test_preview | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_monitor_action_group.email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_action_group) | resource |
| [azurerm_monitor_action_group.error_action_group](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_action_group) | resource |
| [azurerm_monitor_action_group.selfcare_status_dev](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_action_group) | resource |
| [azurerm_monitor_action_group.selfcare_status_uat](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_action_group) | resource |
| [azurerm_monitor_action_group.slack](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_action_group) | resource |
| [azurerm_monitor_metric_alert.functions_exceptions](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_metric_alert) | resource |
| [azurerm_portal_dashboard.monitoring-dashboard](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/portal_dashboard) | resource |
| [azurerm_key_vault_secret.alert_error_notification_email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.alert_error_notification_slack](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.monitor_notification_email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.monitor_notification_opsgenie](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |
| [azurerm_key_vault_secret.monitor_notification_slack_email](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/key_vault_secret) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_application_insights_id"></a> [application\_insights\_id](#input\_application\_insights\_id) | Application Insights ID (from log\_analytics module) | `string` | n/a | yes |
| <a name="input_application_insights_name"></a> [application\_insights\_name](#input\_application\_insights\_name) | Application Insights name (from log\_analytics module) | `string` | n/a | yes |
| <a name="input_cdn_fqdn"></a> [cdn\_fqdn](#input\_cdn\_fqdn) | CDN FQDN for web test | `string` | n/a | yes |
| <a name="input_dns_a_api_fqdn"></a> [dns\_a\_api\_fqdn](#input\_dns\_a\_api\_fqdn) | FQDN of the api DNS A record | `string` | n/a | yes |
| <a name="input_dns_a_api_pnpg_fqdn"></a> [dns\_a\_api\_pnpg\_fqdn](#input\_dns\_a\_api\_pnpg\_fqdn) | FQDN of the api-pnpg DNS A record | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Key Vault ID for reading secrets and writing app insights key | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_monitor_rg_location"></a> [monitor\_rg\_location](#input\_monitor\_rg\_location) | Monitor resource group location (from log\_analytics module) | `string` | n/a | yes |
| <a name="input_monitor_rg_name"></a> [monitor\_rg\_name](#input\_monitor\_rg\_name) | Monitor resource group name (from log\_analytics module) | `string` | n/a | yes |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_selfcare_status_dev_email"></a> [selfcare\_status\_dev\_email](#input\_selfcare\_status\_dev\_email) | Dev status alert email | `string` | `""` | no |
| <a name="input_selfcare_status_dev_slack"></a> [selfcare\_status\_dev\_slack](#input\_selfcare\_status\_dev\_slack) | Dev status alert slack email | `string` | `""` | no |
| <a name="input_selfcare_status_uat_email"></a> [selfcare\_status\_uat\_email](#input\_selfcare\_status\_uat\_email) | UAT status alert email | `string` | `""` | no |
| <a name="input_selfcare_status_uat_slack"></a> [selfcare\_status\_uat\_slack](#input\_selfcare\_status\_uat\_slack) | UAT status alert slack email | `string` | `""` | no |
| <a name="input_subscription_id"></a> [subscription\_id](#input\_subscription\_id) | Azure subscription ID | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_action_group_email_id"></a> [action\_group\_email\_id](#output\_action\_group\_email\_id) | n/a |
| <a name="output_action_group_error_id"></a> [action\_group\_error\_id](#output\_action\_group\_error\_id) | n/a |
| <a name="output_action_group_selfcare_status_dev_id"></a> [action\_group\_selfcare\_status\_dev\_id](#output\_action\_group\_selfcare\_status\_dev\_id) | n/a |
| <a name="output_action_group_selfcare_status_uat_id"></a> [action\_group\_selfcare\_status\_uat\_id](#output\_action\_group\_selfcare\_status\_uat\_id) | n/a |
| <a name="output_action_group_slack_id"></a> [action\_group\_slack\_id](#output\_action\_group\_slack\_id) | n/a |
<!-- END_TF_DOCS -->
