# dns_public

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
| [azurerm_dns_a_record.dns_a_api](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_a_record) | resource |
| [azurerm_dns_a_record.dns_a_api_areariservata](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_a_record) | resource |
| [azurerm_dns_a_record.public_api_pnpg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_a_record) | resource |
| [azurerm_dns_caa_record.caa_areariservata](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_caa_record) | resource |
| [azurerm_dns_caa_record.caa_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_caa_record) | resource |
| [azurerm_dns_cname_record.dkim-aws-ses-areariservata-pagopa-it](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_cname_record) | resource |
| [azurerm_dns_cname_record.dkim-aws-ses-selfcare-pagopa-it](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_cname_record) | resource |
| [azurerm_dns_mx_record.dns-mx-email-areariservata-pagopa-it](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_mx_record) | resource |
| [azurerm_dns_mx_record.dns-mx-email-selfcare-pagopa-it](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_mx_record) | resource |
| [azurerm_dns_ns_record.dev_areariservata](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.dev_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.firmaconio_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.interop_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.io_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.uat_areariservata](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_ns_record.uat_selfcare](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_ns_record) | resource |
| [azurerm_dns_txt_record.dns-txt-areariservata-pagopa-it-aws-ses](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_txt_record) | resource |
| [azurerm_dns_txt_record.dns-txt-email-areariservata-pagopa-it-aws-ses](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_txt_record) | resource |
| [azurerm_dns_txt_record.dns-txt-email-selfcare-pagopa-it-aws-ses](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_txt_record) | resource |
| [azurerm_dns_txt_record.dns-txt-selfcare-pagopa-it-aws-ses](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_txt_record) | resource |
| [azurerm_dns_zone.areariservata_public](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_zone) | resource |
| [azurerm_dns_zone.selfcare_public](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/dns_zone) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_appgateway_public_ip_address"></a> [appgateway\_public\_ip\_address](#input\_appgateway\_public\_ip\_address) | Public IP address of the Application Gateway | `string` | n/a | yes |
| <a name="input_dns_default_ttl_sec"></a> [dns\_default\_ttl\_sec](#input\_dns\_default\_ttl\_sec) | n/a | `number` | `3600` | no |
| <a name="input_dns_ns_interop_selfcare"></a> [dns\_ns\_interop\_selfcare](#input\_dns\_ns\_interop\_selfcare) | NS records for interop delegation | `list(string)` | `null` | no |
| <a name="input_dns_zone_prefix"></a> [dns\_zone\_prefix](#input\_dns\_zone\_prefix) | The dns subdomain. | `string` | `"selfcare"` | no |
| <a name="input_dns_zone_prefix_ar"></a> [dns\_zone\_prefix\_ar](#input\_dns\_zone\_prefix\_ar) | The dns subdomain for areariservata. | `string` | `"areariservat"` | no |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | Domain for delegation | `string` | `"pagopa.it"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | From network module | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_areariservata_public_dns_zone_name"></a> [areariservata\_public\_dns\_zone\_name](#output\_areariservata\_public\_dns\_zone\_name) | n/a |
| <a name="output_dns_a_api_fqdn"></a> [dns\_a\_api\_fqdn](#output\_dns\_a\_api\_fqdn) | n/a |
| <a name="output_dns_a_api_pnpg_fqdn"></a> [dns\_a\_api\_pnpg\_fqdn](#output\_dns\_a\_api\_pnpg\_fqdn) | n/a |
| <a name="output_selfcare_public_dns_zone_name"></a> [selfcare\_public\_dns\_zone\_name](#output\_selfcare\_public\_dns\_zone\_name) | n/a |
| <a name="output_selfcare_public_dns_zone_resource_group_name"></a> [selfcare\_public\_dns\_zone\_resource\_group\_name](#output\_selfcare\_public\_dns\_zone\_resource\_group\_name) | n/a |
<!-- END_TF_DOCS -->
