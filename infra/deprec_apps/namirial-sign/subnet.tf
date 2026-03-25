resource "azurerm_subnet" "namirial_sws_snet" {
  name                                          = "${local.project}-namirial-sws-snet"
  resource_group_name                           = "${local.project}-vnet-rg"
  virtual_network_name                          = data.azurerm_virtual_network.vnet_selc.name
  address_prefixes                              = var.cidr_subnet_namirial_sws
  private_link_service_network_policies_enabled = true
  private_endpoint_network_policies_enabled     = false

  delegation {
    name = "delegation"
    service_delegation {
      name    = "Microsoft.ContainerInstance/containerGroups"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}