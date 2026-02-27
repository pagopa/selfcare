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
# network
###############################################################################

module "network" {
  source = "../_modules/data/network"

  prefix                         = local.prefix
  env_short                      = local.env_short
  redis_private_endpoint_enabled = local.redis_private_endpoint_enabled
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

###############################################################################
# Logs storage
###############################################################################

module "logs_storage" {
  source = "../_modules/storage_account_template"

  project              = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}"
  location             = local.location
  tags                 = local.tags
  name                 = "logs"
  storage_account_name = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}-st-logs"

  account_replication_type      = "LRS"
  enable_versioning             = false
  advanced_threat_protection    = true
  delete_retention_days         = 14
  public_network_access_enabled = false

  key_vault_id = module.key_vault.key_vault_id
  rg_vnet_name = data.azurerm_virtual_network.vnet.resource_group_name
  vnet_name    = data.azurerm_virtual_network.vnet.name

  cidr_subnet                       = local.cidr_subnet_pnpg_logs_storage
  private_endpoint_network_policies = local.private_endpoint_network_policies
  private_dns_zone_ids              = [module.network.privatelink_blob_core_windows_net.id]

  enable_management_lock           = true
  enable_spid_logs_encryption_keys = true
}

###############################################################################
# Spid
###############################################################################

module "spid_logs_encryption_keys" {
  source = "../_modules/spid_logs_encryption_keys"

  key_vault_id = module.key_vault.key_vault_id
  tags         = local.tags
}


###############################################################################
# Spid Test Environment
###############################################################################

module "spid_test_env" {
  source = "../_modules/spid_testenv"

  enable_spid_test = true

  name                        = "${local.prefix}-${local.env_short}-${local.app_domain}-spid-testenv"
  location                    = local.location
  key_vault_id                = module.key_vault.key_vault_id
  hub_spid_login_metadata_url = "https://api-pnpg.${local.dns_zone_prefix}.${local.external_domain}/spid/v1/metadata"

  spid_testenv_local_config_dir = "${path.module}/spid_testenv_conf"


  tags = local.tags
}


