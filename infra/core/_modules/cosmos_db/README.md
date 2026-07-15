# cosmos_db

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
| <a name="module_cosmosdb_account_mongodb"></a> [cosmosdb\_account\_mongodb](#module\_cosmosdb\_account\_mongodb) | github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_account | v9.6.1 |
| <a name="module_cosmosdb_mongodb_snet"></a> [cosmosdb\_mongodb\_snet](#module\_cosmosdb\_mongodb\_snet) | github.com/pagopa/terraform-azurerm-v4.git//subnet | v9.6.1 |

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_key_vault_secret.cosmosdb_account_mongodb_connection_strings](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/key_vault_secret) | resource |
| [azurerm_resource_group.mongodb_rg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/resource_group) | resource |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_cidr_subnet_cosmosdb_mongodb"></a> [cidr\_subnet\_cosmosdb\_mongodb](#input\_cidr\_subnet\_cosmosdb\_mongodb) | CIDR block for CosmosDB MongoDB subnet | `list(string)` | n/a | yes |
| <a name="input_cosmosdb_mongodb_additional_geo_locations"></a> [cosmosdb\_mongodb\_additional\_geo\_locations](#input\_cosmosdb\_mongodb\_additional\_geo\_locations) | The name of the Azure region to host replicated data and the priority to apply starting from 1. Not used when cosmosdb\_mongodb\_enable\_serverless | <pre>list(object({<br/>    location          = string<br/>    failover_priority = number<br/>    zone_redundant    = bool<br/>  }))</pre> | `[]` | no |
| <a name="input_cosmosdb_mongodb_consistency_policy"></a> [cosmosdb\_mongodb\_consistency\_policy](#input\_cosmosdb\_mongodb\_consistency\_policy) | n/a | <pre>object({<br/>    consistency_level       = string<br/>    max_interval_in_seconds = number<br/>    max_staleness_prefix    = number<br/>  })</pre> | <pre>{<br/>  "consistency_level": "Session",<br/>  "max_interval_in_seconds": null,<br/>  "max_staleness_prefix": null<br/>}</pre> | no |
| <a name="input_cosmosdb_mongodb_enable_autoscaling"></a> [cosmosdb\_mongodb\_enable\_autoscaling](#input\_cosmosdb\_mongodb\_enable\_autoscaling) | It will enable autoscaling mode. If true, cosmosdb\_mongodb\_throughput must be unset | `bool` | `false` | no |
| <a name="input_cosmosdb_mongodb_enable_free_tier"></a> [cosmosdb\_mongodb\_enable\_free\_tier](#input\_cosmosdb\_mongodb\_enable\_free\_tier) | Enable Free Tier pricing option for this Cosmos DB account | `bool` | `true` | no |
| <a name="input_cosmosdb_mongodb_extra_capabilities"></a> [cosmosdb\_mongodb\_extra\_capabilities](#input\_cosmosdb\_mongodb\_extra\_capabilities) | Extra capabilities for the CosmosDB MongoDB account (e.g. EnableServerless) | `list(string)` | `[]` | no |
| <a name="input_cosmosdb_mongodb_main_geo_location_zone_redundant"></a> [cosmosdb\_mongodb\_main\_geo\_location\_zone\_redundant](#input\_cosmosdb\_mongodb\_main\_geo\_location\_zone\_redundant) | Enable zone redundant Comsmos DB | `bool` | n/a | yes |
| <a name="input_cosmosdb_mongodb_max_throughput"></a> [cosmosdb\_mongodb\_max\_throughput](#input\_cosmosdb\_mongodb\_max\_throughput) | The maximum throughput of the MongoDB database (RU/s). Must be between 4,000 and 1,000,000. Must be set in increments of 1,000. Conflicts with throughput | `number` | `4000` | no |
| <a name="input_cosmosdb_mongodb_offer_type"></a> [cosmosdb\_mongodb\_offer\_type](#input\_cosmosdb\_mongodb\_offer\_type) | n/a | `string` | `"Standard"` | no |
| <a name="input_cosmosdb_mongodb_private_endpoint_enabled"></a> [cosmosdb\_mongodb\_private\_endpoint\_enabled](#input\_cosmosdb\_mongodb\_private\_endpoint\_enabled) | Enable private endpoint for Comsmos DB | `bool` | `true` | no |
| <a name="input_cosmosdb_mongodb_public_network_access_enabled"></a> [cosmosdb\_mongodb\_public\_network\_access\_enabled](#input\_cosmosdb\_mongodb\_public\_network\_access\_enabled) | Whether or not public network access is allowed for this CosmosDB account | `bool` | `false` | no |
| <a name="input_cosmosdb_mongodb_throughput"></a> [cosmosdb\_mongodb\_throughput](#input\_cosmosdb\_mongodb\_throughput) | The throughput of the MongoDB database (RU/s). Must be set in increments of 100. The minimum value is 400. This must be set upon database creation otherwise it cannot be updated without a manual terraform destroy-apply. | `number` | `400` | no |
| <a name="input_cosmosdb_private_endpoint_mongo_name"></a> [cosmosdb\_private\_endpoint\_mongo\_name](#input\_cosmosdb\_private\_endpoint\_mongo\_name) | Name of the private endpoint for MongoDB API | `string` | n/a | yes |
| <a name="input_cosmosdb_private_service_connection_mongo_name"></a> [cosmosdb\_private\_service\_connection\_mongo\_name](#input\_cosmosdb\_private\_service\_connection\_mongo\_name) | Name of the private service connection for MongoDB API | `string` | n/a | yes |
| <a name="input_env_short"></a> [env\_short](#input\_env\_short) | n/a | `string` | n/a | yes |
| <a name="input_external_domain"></a> [external\_domain](#input\_external\_domain) | n/a | `string` | `"pagopa.it"` | no |
| <a name="input_key_vault_id"></a> [key\_vault\_id](#input\_key\_vault\_id) | Key Vault ID for storing connection strings | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | `"westeurope"` | no |
| <a name="input_prefix"></a> [prefix](#input\_prefix) | n/a | `string` | `"selc"` | no |
| <a name="input_private_endpoint_network_policies"></a> [private\_endpoint\_network\_policies](#input\_private\_endpoint\_network\_policies) | Private endpoint network policies for subnets | `string` | `"Enabled"` | no |
| <a name="input_privatelink_mongo_cosmos_azure_com_id"></a> [privatelink\_mongo\_cosmos\_azure\_com\_id](#input\_privatelink\_mongo\_cosmos\_azure\_com\_id) | Private DNS Zone ID for privatelink.mongo.cosmos.azure.com | `string` | n/a | yes |
| <a name="input_project"></a> [project](#input\_project) | n/a | `string` | n/a | yes |
| <a name="input_rg_vnet_name"></a> [rg\_vnet\_name](#input\_rg\_vnet\_name) | VNet resource group name | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | n/a | `map(any)` | `{}` | no |
| <a name="input_vnet_name"></a> [vnet\_name](#input\_vnet\_name) | VNet name | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
