###############################################################################
# Azure Group
###############################################################################

module "azure_group" {
  source = "../_modules/data/azure_group"

  prefix    = local.prefix
  env_short = local.env_short
}

###############################################################################
# vnet
###############################################################################

data "azurerm_virtual_network" "vnet" {
  name                = "${local.prefix}-${local.env_short}-vnet"
  resource_group_name = "${local.prefix}-${local.env_short}-vnet-rg"
}

###############################################################################
# key_vault
###############################################################################
module "key_vault" {
  source = "../_modules/key_vault"

  project   = "${local.prefix}-${local.env_short}-${local.app_domain}"
  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  azdo_sp_tls_cert_enabled                         = local.azdo_sp_tls_cert_enabled
  azuread_service_principal_azure_cdn_frontdoor_id = "f3b3f72f-4770-47a5-8c1e-aa298003be12"
}


###############################################################################
# redis
###############################################################################
module "redis" {
  source = "../_modules/redis"

  project  = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}"
  location = local.location

  rg_redis                          = data.azurerm_virtual_network.vnet.resource_group_name
  rg_vnet_name                      = data.azurerm_virtual_network.vnet.resource_group_name
  vnet_name                         = data.azurerm_virtual_network.vnet.name
  vnet_id                           = data.azurerm_virtual_network.vnet.id
  cidr_subnet_redis                 = local.cidr_subnet_pnpg_redis
  tags                              = local.tags
  redis_private_endpoint_enabled    = local.redis_private_endpoint_enabled
  private_endpoint_network_policies = "Disabled"
  key_vault_id                      = module.key_vault.key_vault_id

  redis_sku_name = local.redis_sku_name
  redis_family   = local.redis_family
  redis_capacity = local.redis_capacity
  redis_version  = local.redis_version
}
