output "config" {
  value = {
    prefix         = local.prefix
    env_short      = local.env_short
    location_short = local.location_short
    location       = local.location
    domain         = local.domain

    dns_zone_prefix                = local.dns_zone_prefix
    api_dns_zone_prefix            = local.api_dns_zone_prefix
    external_domain                = local.external_domain
    apim_name                      = local.apim_name
    apim_rg                        = local.apim_rg
    project                        = local.project
    mongo_db                       = local.mongo_db
    container_app_environment_name = local.container_app_environment_name
    ca_resource_group_name         = local.ca_resource_group_name
    private_dns_name_domain        = local.private_dns_name_domain
    container_app                  = local.container_app
    quarkus_health_probes          = local.quarkus_health_probes
    container_app_onboarding_bff   = local.container_app
    tags                           = local.tags
    cidr_subnet_document_storage   = local.cidr_subnet_document_storage
    key_vault_resource_group_name  = local.key_vault_resource_group_name
    key_vault_name                 = local.key_vault_name

    resource_group_name_vnet = local.resource_group_name_vnet
    image_tag_latest         = local.image_tag_latest
  }
}

output "key_vault_id" {
  value = data.azurerm_key_vault.key_vault.id
}

output "vnet_selc_name" {
  value = data.azurerm_virtual_network.vnet_selc.name
}

output "vnet_resource_group_name" {
  value = data.azurerm_virtual_network.vnet_selc.resource_group_name
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}

output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}
