
###############################################################################
# CDC Managed Identity
###############################################################################

resource "azurerm_user_assigned_identity" "cdc_identity" {
  name                = "${var.prefix}-${var.env_short}${var.is_pnpg ? "-pnpg" : ""}-cdc-managed_identity"
  location            = var.location
  resource_group_name = var.resource_group_name
  tags                = var.tags
}

resource "azurerm_management_lock" "cdc_identity_lock" {
  name       = azurerm_user_assigned_identity.cdc_identity.name
  scope      = azurerm_user_assigned_identity.cdc_identity.id
  lock_level = "CanNotDelete"
  notes      = "Lock for the CDC managed identity"
}

data "azurerm_storage_account" "cdc_table_storage" {
  name                = "${var.prefix}${var.env_short}${var.is_pnpg ? "weupnpgcheckoutst01" : "weuarcheckoutst01"}"
  resource_group_name = "${var.prefix}-${var.env_short}${var.is_pnpg ? "-weu-pnpg-checkout-fe-rg" : "-checkout-fe-rg"}"
}

resource "azurerm_role_assignment" "cdc_identity_role_assignment" {
  scope                = data.azurerm_storage_account.cdc_table_storage.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = azurerm_user_assigned_identity.cdc_identity.principal_id
}
