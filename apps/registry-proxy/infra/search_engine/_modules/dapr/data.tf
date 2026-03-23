data "azurerm_container_app_environment" "cae" {
  name                = var.cae_name
  resource_group_name = var.cae_rg_name
}

data "azurerm_container_app" "ca" {
  name                = var.ca_name
  resource_group_name = var.ca_rg_name
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${var.cae_name}-managed_identity"
  resource_group_name = var.cae_rg_name
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault_resource_group_name
  name                = var.key_vault_name
}

data "azurerm_key_vault_secret" "event_hub_consumer_key" {
  name         = var.key_vault_event_hub_consumer_key
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_resource_group" "rg_vnet" {
  name = "${var.prefix}-${var.env_short}-vnet-rg"
}

data "azurerm_virtual_network" "vnet" {
  name                = "${var.prefix}-${var.env_short}-vnet"
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}

data "azurerm_subnet" "redis_snet" {
  name                 = "${var.prefix}-${var.env_short}-redis-snet"
  virtual_network_name = data.azurerm_virtual_network.vnet.name
  resource_group_name  = data.azurerm_resource_group.rg_vnet.name
}

data "azurerm_private_dns_zone" "privatelink_redis_cache_windows_net" {
  count               = var.redis_private_endpoint_enabled == true ? 1 : 0
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}