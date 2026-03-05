locals {
  project      = "${var.prefix}-${var.env_short}"
  project_pair = "${var.prefix}-${var.env_short}-${var.location_pair_short}"
}

# Core VNet
resource "azurerm_resource_group" "rg_vnet" {
  name     = "${local.project}-vnet-rg"
  location = var.location
  tags     = var.tags
}

module "vnet" {
  source               = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network?ref=v8.5.3"
  name                 = "${local.project}-vnet"
  location             = azurerm_resource_group.rg_vnet.location
  resource_group_name  = azurerm_resource_group.rg_vnet.name
  address_space        = var.cidr_vnet
  ddos_protection_plan = var.ddos_protection_plan
  tags                 = var.tags
}

module "private_endpoints_subnet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v8.5.3"
  name                              = "${local.project}-private-endpoints-snet"
  address_prefixes                  = var.cidr_subnet_private_endpoints
  resource_group_name               = azurerm_resource_group.rg_vnet.name
  virtual_network_name              = module.vnet.name
  private_endpoint_network_policies = var.private_endpoint_network_policies

  service_endpoints = [
    "Microsoft.Storage",
  ]
}

# Pair VNet
resource "azurerm_resource_group" "rg_pair_vnet" {
  name     = "${local.project_pair}-vnet-rg"
  location = var.location_pair
  tags     = var.tags
}

module "vnet_pair" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network?ref=v8.5.3"
  name                = "${local.project_pair}-vnet"
  location            = azurerm_resource_group.rg_pair_vnet.location
  resource_group_name = azurerm_resource_group.rg_pair_vnet.name
  address_space       = var.cidr_pair_vnet
  tags                = var.tags
}

# AKS VNet
resource "azurerm_resource_group" "rg_vnet_aks" {
  name     = "${local.project}-${var.location_short}-vnet-rg"
  location = var.location
  tags     = var.tags
}

module "vnet_aks_platform" {
  source               = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network?ref=v8.5.3"
  name                 = "${local.project}-${var.location_short}-aks-${var.aks_platform_env}-vnet"
  location             = var.location
  resource_group_name  = azurerm_resource_group.rg_vnet_aks.name
  address_space        = var.cidr_aks_platform_vnet
  ddos_protection_plan = var.ddos_protection_plan
  tags                 = var.tags
}

resource "azurerm_public_ip" "outbound_ip_aks_platform" {
  name                = "${local.project}-${var.location_short}-aks-platform-outbound-pip"
  domain_name_label   = "${local.project}-${var.location_short}-aks-platform-outbound-pip"
  location            = azurerm_resource_group.rg_vnet_aks.location
  resource_group_name = azurerm_resource_group.rg_vnet_aks.name
  sku                 = "Standard"
  allocation_method   = "Static"
  zones               = ["1", "2", "3"]
  tags                = var.tags
}

# Application Gateway Public IP (shared by appgateway and dns_public)
resource "azurerm_public_ip" "appgateway_public_ip" {
  name                = "${local.project}-appgateway-pip"
  resource_group_name = azurerm_resource_group.rg_vnet.name
  location            = azurerm_resource_group.rg_vnet.location
  sku                 = "Standard"
  allocation_method   = "Static"
  zones               = ["1", "2", "3"]
  tags                = var.tags
}

# Peerings
module "vnet_peering_core_2_aks" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering?ref=v8.5.3"

  source_resource_group_name       = azurerm_resource_group.rg_vnet.name
  source_virtual_network_name      = module.vnet.name
  source_remote_virtual_network_id = module.vnet.id
  source_allow_gateway_transit     = true

  target_resource_group_name       = azurerm_resource_group.rg_vnet_aks.name
  target_virtual_network_name      = module.vnet_aks_platform.name
  target_remote_virtual_network_id = module.vnet_aks_platform.id
  target_use_remote_gateways       = true
}

module "vnet_peering_pair_vs_core" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering?ref=v8.5.3"

  source_resource_group_name       = azurerm_resource_group.rg_pair_vnet.name
  source_virtual_network_name      = module.vnet_pair.name
  source_remote_virtual_network_id = module.vnet_pair.id
  source_allow_gateway_transit     = false
  source_use_remote_gateways       = true
  source_allow_forwarded_traffic   = true

  target_resource_group_name       = azurerm_resource_group.rg_vnet.name
  target_virtual_network_name      = module.vnet.name
  target_remote_virtual_network_id = module.vnet.id
  target_allow_gateway_transit     = true
  target_use_remote_gateways       = false
  target_allow_forwarded_traffic   = true
}

module "vnet_peering_pair_vs_aks" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//virtual_network_peering?ref=v8.5.3"

  source_resource_group_name       = azurerm_resource_group.rg_pair_vnet.name
  source_virtual_network_name      = module.vnet_pair.name
  source_remote_virtual_network_id = module.vnet_pair.id
  source_allow_gateway_transit     = false
  source_use_remote_gateways       = false
  source_allow_forwarded_traffic   = true

  target_resource_group_name       = azurerm_resource_group.rg_vnet_aks.name
  target_virtual_network_name      = module.vnet_aks_platform.name
  target_remote_virtual_network_id = module.vnet_aks_platform.id
  target_allow_gateway_transit     = true
  target_use_remote_gateways       = false
  target_allow_forwarded_traffic   = true
}
