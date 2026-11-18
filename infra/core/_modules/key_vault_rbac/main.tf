locals {
  naming = {
    prefix          = var.prefix
    environment     = var.env_short
    location        = var.location
    domain          = var.domain
    instance_number = "01"
  }
}

resource "azurerm_key_vault" "this" {
  name = provider::dx::resource_name(merge(local.naming, {
    resource_type = "key_vault"
  }))
  location                      = var.location
  resource_group_name           = var.resource_group_name
  tenant_id                     = var.tenant_id
  sku_name                      = "standard"
  rbac_authorization_enabled    = true
  purge_protection_enabled      = true
  soft_delete_retention_days    = 90
  public_network_access_enabled = false

  network_acls {
    bypass         = "None"
    default_action = "Deny"
  }

  tags = var.tags
}

resource "azurerm_role_assignment" "admin_secrets" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = var.adgroup_admin.role_definition_name
  principal_id         = var.adgroup_admin.object_id
}

resource "azurerm_role_assignment" "developers_secrets" {
  scope                = azurerm_key_vault.this.id
  role_definition_name = var.adgroup_developers.role_definition_name
  principal_id         = var.adgroup_developers.object_id
}

resource "azurerm_private_dns_zone" "key_vault" {
  name                = "privatelink.vaultcore.azure.net"
  resource_group_name = var.networking.resource_group_name

  tags = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "key_vault" {
  name                  = azurerm_key_vault.this.name
  resource_group_name   = var.networking.resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.key_vault.name
  virtual_network_id    = var.networking.virtual_network_id
  registration_enabled  = false

  tags = var.tags
}

resource "azurerm_private_endpoint" "key_vault" {
  name = provider::dx::resource_name(merge(local.naming, {
    resource_type = "key_vault_private_endpoint"
  }))
  location            = var.location
  resource_group_name = var.networking.resource_group_name
  subnet_id           = var.networking.private_endpoint_subnet_id

  private_service_connection {
    name                           = azurerm_key_vault.this.name
    private_connection_resource_id = azurerm_key_vault.this.id
    is_manual_connection           = false
    subresource_names              = ["vault"]
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.key_vault.id]
  }

  tags = var.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.key_vault]
}
