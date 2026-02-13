locals {
  project                                 = "${var.prefix}-${var.env_short}"
  internal_selfcare_private_domain        = var.env_short == "p" ? "internal.selfcare.pagopa.it" : "internal.${var.env}.selfcare.pagopa.it"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
}

#
# internal.selfcare.pagopa.it
#
resource "azurerm_private_dns_zone" "internal_private_dns_zone" {
  name                = local.internal_selfcare_private_domain
  resource_group_name = var.rg_vnet_name
}

resource "azurerm_private_dns_a_record" "selc" {
  name                = "selc"
  zone_name           = azurerm_private_dns_zone.internal_private_dns_zone.name
  resource_group_name = var.rg_vnet_name
  ttl                 = var.dns_default_ttl_sec
  records             = [var.reverse_proxy_ip]
  tags                = var.tags
}

# DNS private Links - core vnet
resource "azurerm_private_dns_zone_virtual_network_link" "internal_env_selfcare_pagopa_it_2_vnet_core" {
  name                  = "${local.project}-link-vnet-core"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.internal_private_dns_zone.name
  virtual_network_id    = var.vnet_id
}

resource "azurerm_private_dns_zone_virtual_network_link" "internal_env_selfcare_pagopa_it_2_vnet_core_pair" {
  name                  = "${local.project}-pair-link-vnet-core"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.internal_private_dns_zone.name
  virtual_network_id    = var.vnet_pair_id
}

# COSMOS
resource "azurerm_private_dns_zone" "privatelink_documents_azure_com" {
  name                = "privatelink.documents.azure.com"
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_documents_azure_com_vnet" {
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_documents_azure_com.name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_documents_azure_com_vnet_pair" {
  name                  = var.vnet_pair_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_documents_azure_com.name
  virtual_network_id    = var.vnet_pair_id
  registration_enabled  = false
  tags                  = var.tags
}

# COSMOS MONGO
resource "azurerm_private_dns_zone" "privatelink_mongo_cosmos_azure_com" {
  name                = "privatelink.mongo.cosmos.azure.com"
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_mongo_cosmos_azure_com_vnet" {
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com.name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_mongo_cosmos_azure_com_vnet_pair" {
  name                  = var.vnet_pair_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com.name
  virtual_network_id    = var.vnet_pair_id
  registration_enabled  = false
  tags                  = var.tags
}

# STORAGE ACCOUNT / BLOB
resource "azurerm_private_dns_zone" "privatelink_blob_core_windows_net" {
  name                = "privatelink.blob.core.windows.net"
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_blob_core_windows_net_vnet" {
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_blob_core_windows_net.name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_blob_core_windows_net_vnet_pair" {
  name                  = var.vnet_pair_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_blob_core_windows_net.name
  virtual_network_id    = var.vnet_pair_id
  registration_enabled  = false
  tags                  = var.tags
}

# REDIS
resource "azurerm_private_dns_zone" "privatelink_redis_cache_windows_net" {
  count               = var.redis_private_endpoint_enabled ? 1 : 0
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_redis_cache_windows_net_vnet" {
  count                 = var.redis_private_endpoint_enabled ? 1 : 0
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_redis_cache_windows_net[0].name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_redis_cache_windows_net_vnet_pair" {
  count                 = var.redis_private_endpoint_enabled ? 1 : 0
  name                  = var.vnet_pair_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_redis_cache_windows_net[0].name
  virtual_network_id    = var.vnet_pair_id
  registration_enabled  = false
  tags                  = var.tags
}

# SERVICE BUS
resource "azurerm_private_dns_zone" "privatelink_servicebus_windows_net" {
  name                = "privatelink.servicebus.windows.net"
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_servicebus_windows_net_vnet" {
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_servicebus_windows_net.name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_servicebus_windows_net_vnet_pair" {
  name                  = var.vnet_pair_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_servicebus_windows_net.name
  virtual_network_id    = var.vnet_pair_id
  registration_enabled  = false
  tags                  = var.tags
}

# CONTAINER APPS
resource "azurerm_private_dns_zone" "private_azurecontainerapps_io" {
  name                = local.container_app_environment_dns_zone_name
  resource_group_name = var.rg_vnet_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_azurecontainerapps_io_vnet_pair" {
  name                  = var.vnet_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.private_azurecontainerapps_io.name
  virtual_network_id    = var.vnet_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_azurecontainerapps_io_weu_vnet_pair" {
  name                  = var.vnet_aks_platform_name
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.private_azurecontainerapps_io.name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}

#
# AKS VNet DNS links (from 03_network_aks_domains.tf)
#

# INTERNAL
resource "azurerm_private_dns_zone_virtual_network_link" "internal_env_selfcare_pagopa_it_2_aks_vnet" {
  name                  = "${local.project}-integration-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.internal_private_dns_zone.name
  virtual_network_id    = var.vnet_aks_platform_id
}

# STORAGE
resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_documents_azure_com_vnet_vs_aks_vnet" {
  name                  = "${local.project}-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_documents_azure_com.name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}

# COSMOS-MONGO
resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_mongo_cosmos_azure_com_vnet_vs_aks_vnet" {
  name                  = "${local.project}-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com.name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}

# BLOB
resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_blob_core_windows_net_vnet_vs_aks_vnet" {
  name                  = "${local.project}-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_blob_core_windows_net.name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}

# REDIS
resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_redis_cache_windows_net_vnet_vs_aks_vnet" {
  count                 = var.redis_private_endpoint_enabled ? 1 : 0
  name                  = "${local.project}-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_redis_cache_windows_net[0].name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}

# SERVICE BUS
resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_servicebus_windows_net_vnet_vs_aks_vnet" {
  name                  = "${local.project}-aks-platform-vnet"
  resource_group_name   = var.rg_vnet_name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_servicebus_windows_net.name
  virtual_network_id    = var.vnet_aks_platform_id
  registration_enabled  = false
  tags                  = var.tags
}
