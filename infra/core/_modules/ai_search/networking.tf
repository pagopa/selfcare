module "srch_snet" {
  count                             = var.srch_private_endpoint_enabled ? 1 : 0
  source                            = "github.com/pagopa/terraform-azurerm-v4//subnet?ref=v7.20.0"
  name                              = "${var.project}-srch-snet-01"
  resource_group_name               = data.azurerm_resource_group.rg_vnet.name
  virtual_network_name              = data.azurerm_virtual_network.vnet.name
  address_prefixes                  = var.cidr_subnet
  private_endpoint_network_policies = "Enabled"
}

resource "azurerm_private_dns_zone" "privatelink_srch_azure_com" {
  count               = var.srch_private_endpoint_enabled ? 1 : 0
  name                = "privatelink.search.windows.net"
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "privatelink_srch_windows_net_vnet" {
  count                 = var.srch_private_endpoint_enabled ? 1 : 0
  name                  = "${var.project}-dns-vnl"
  resource_group_name   = data.azurerm_resource_group.rg_vnet.name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink_srch_azure_com[0].name
  virtual_network_id    = data.azurerm_virtual_network.vnet.id
  registration_enabled  = false

  tags = var.tags
}

resource "azurerm_private_endpoint" "srch_pep" {
  count               = var.srch_private_endpoint_enabled ? 1 : 0
  name                = "${var.project}-srch-pep-01"
  location            = var.location
  resource_group_name = azurerm_resource_group.srch_rg.name
  subnet_id           = module.srch_snet[0].id
  depends_on          = [azurerm_search_service.srch_service]

  private_service_connection {
    name                           = azurerm_search_service.srch_service.name
    private_connection_resource_id = azurerm_search_service.srch_service.id
    is_manual_connection           = false
    subresource_names              = ["searchService"]
  }

  private_dns_zone_group {
    name                 = "${var.project}-srch-dns-zone-group-01"
    private_dns_zone_ids = [azurerm_private_dns_zone.privatelink_srch_azure_com[0].id]
  }
}

resource "azurerm_private_dns_a_record" "dns_a_record" {
  count               = var.srch_private_endpoint_enabled ? 1 : 0
  name                = azurerm_search_service.srch_service.name
  zone_name           = azurerm_private_dns_zone.privatelink_srch_azure_com[0].name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
  ttl                 = 10
  records             = [azurerm_private_endpoint.srch_pep[0].private_service_connection[0].private_ip_address]
}

