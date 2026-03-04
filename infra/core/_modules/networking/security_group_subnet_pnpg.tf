resource "azurerm_network_security_group" "subnet_nsg" {
  count               = var.core_vnet ? 0 : 1
  name                = "${var.project}-pnpg-container-app-nsg"
  location            = data.azurerm_virtual_network.vnet.location
  resource_group_name = data.azurerm_virtual_network.vnet.resource_group_name
}

resource "azurerm_network_security_rule" "cae_subnet_outbound_rule" {
  count                       = var.core_vnet ? 0 : 1
  name                        = "BlockAnyCidrCaeSubnetOutBound"
  priority                    = 100
  direction                   = "Outbound"
  access                      = "Deny"
  protocol                    = "*"
  source_port_range           = "*"
  destination_port_range      = "*"
  source_address_prefix       = "*"
  destination_address_prefix  = var.cidr_subnet_cae
  resource_group_name         = data.azurerm_virtual_network.vnet.resource_group_name
  network_security_group_name = azurerm_network_security_group.subnet_nsg[0].name
}

resource "azurerm_network_security_rule" "cae_subnet_inbound_rule" {
  count                       = var.core_vnet ? 0 : 1
  name                        = "BlockCidrCaeSubnetAnyInBound"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Deny"
  protocol                    = "*"
  source_port_range           = "*"
  destination_port_range      = "*"
  source_address_prefix       = var.cidr_subnet_cae
  destination_address_prefix  = "*"
  resource_group_name         = data.azurerm_virtual_network.vnet.resource_group_name
  network_security_group_name = azurerm_network_security_group.subnet_nsg[0].name
}

resource "azurerm_subnet_network_security_group_association" "nsg_cae_subnet_association" {
  count                     = var.core_vnet ? 0 : 1
  subnet_id                 = azurerm_subnet.container_app_snet.id
  network_security_group_id = azurerm_network_security_group.subnet_nsg[0].id
}
