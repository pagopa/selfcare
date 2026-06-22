
resource "azurerm_resource_group" "user_managed_identity_rg" {
  name     = "${var.prefix}-${var.env_short}-${var.domain}-user-managed-identity-rg"
  location = var.location
}

###############################################################################
# Product Storage Table Managed Identity
###############################################################################

resource "azurerm_user_assigned_identity" "product_storage_table_identity" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-product-storage-table-managed-identity"
  location            = var.location
  resource_group_name = azurerm_resource_group.user_managed_identity_rg.name
  tags                = var.tags
}

resource "azurerm_management_lock" "product_storage_table_identity_lock" {
  name       = azurerm_user_assigned_identity.product_storage_table_identity.name
  scope      = azurerm_user_assigned_identity.product_storage_table_identity.id
  lock_level = "CanNotDelete"
  notes      = "Lock for the Product Storage Table Managed Identity"
}

data "azurerm_storage_account" "product_storage_table" {
  name                = var.product_storage_name
  resource_group_name = var.product_storage_rg
}

resource "azurerm_role_assignment" "product_storage_table_identity_role_assignment" {
  scope                = data.azurerm_storage_account.product_storage_table.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = azurerm_user_assigned_identity.product_storage_table_identity.principal_id
}

###############################################################################
# Product Storage Blob Managed Identity
###############################################################################

resource "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-product-storage-blob-managed-identity"
  location            = var.location
  resource_group_name = azurerm_resource_group.user_managed_identity_rg.name
  tags                = var.tags
}

resource "azurerm_management_lock" "product_storage_blob_identity_lock" {
  name       = azurerm_user_assigned_identity.product_storage_blob_identity.name
  scope      = azurerm_user_assigned_identity.product_storage_blob_identity.id
  lock_level = "CanNotDelete"
  notes      = "Lock for the Product Storage Blob Managed Identity"
}

resource "azurerm_role_assignment" "product_storage_blob_identity_role_assignment" {
  scope                = data.azurerm_storage_account.product_storage_table.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.product_storage_blob_identity.principal_id
}

