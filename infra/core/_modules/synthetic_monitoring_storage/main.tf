locals {
  storage_account_name = "${replace(var.project, "-", "")}synthmon"
}

resource "azurerm_storage_account" "this" {
  name                          = local.storage_account_name
  resource_group_name           = var.resource_group_name
  location                      = var.location
  account_tier                  = "Standard"
  account_replication_type      = "ZRS"
  account_kind                  = "StorageV2"
  min_tls_version               = "TLS1_2"
  public_network_access_enabled = false
  # The AzureRM provider manages Table data-plane resources (table and entities) with Shared Key
  # only: the Set Table ACL operation does not support Entra ID, so `storage_use_azuread` is
  # ignored and Terraform fails with a 403 when shared keys are disabled.
  # Keys stay enabled for Terraform, while `default_to_oauth_authentication` keeps Entra ID as the
  # default and the synthetic monitoring job always authenticates with its managed identity.
  shared_access_key_enabled       = true
  default_to_oauth_authentication = true
  allow_nested_items_to_be_public = false

  tags = var.tags
}

resource "azurerm_private_dns_zone" "table" {
  name                = "privatelink.table.core.windows.net"
  resource_group_name = var.virtual_network_resource_group_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "table" {
  name                  = var.virtual_network_name
  resource_group_name   = var.virtual_network_resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.table.name
  virtual_network_id    = var.virtual_network_id
  registration_enabled  = false
  tags                  = var.tags
}

resource "azurerm_private_endpoint" "table" {
  name                = "${var.project}-synthetic-monitoring-table-pep"
  location            = var.location
  resource_group_name = var.resource_group_name
  subnet_id           = var.private_endpoint_subnet_id

  private_service_connection {
    name                           = "${var.project}-synthetic-monitoring-table"
    private_connection_resource_id = azurerm_storage_account.this.id
    is_manual_connection           = false
    subresource_names              = ["table"]
  }

  private_dns_zone_group {
    name                 = "private-dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.table.id]
  }

  tags = var.tags
}
