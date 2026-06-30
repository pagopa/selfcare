
resource "azurerm_resource_group" "user_managed_identity_rg" {
  name     = "${var.prefix}-${var.env_short}-${var.domain}-user-managed-identity-rg"
  location = var.location
}

data "azurerm_storage_account" "product_storage" {
  name                = var.product_storage_name
  resource_group_name = var.product_storage_rg
}

data "azurerm_storage_account" "documents_storage" {
  name                = var.documents_storage_name
  resource_group_name = var.documents_storage_rg
}

data "azurerm_storage_account" "web_storage" {
  name                = var.web_storage_name
  resource_group_name = var.web_storage_rg
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

resource "azurerm_role_assignment" "product_storage_table_identity_role_assignment" {
  scope                = data.azurerm_storage_account.product_storage.id
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
  scope                = data.azurerm_storage_account.product_storage.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.product_storage_blob_identity.principal_id
}

###############################################################################
# Documents Storage Blob Managed Identity
###############################################################################

resource "azurerm_user_assigned_identity" "documents_storage_blob_identity" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-documents-storage-blob-managed-identity"
  location            = var.location
  resource_group_name = azurerm_resource_group.user_managed_identity_rg.name
  tags                = var.tags
}

resource "azurerm_management_lock" "documents_storage_blob_identity_lock" {
  name       = azurerm_user_assigned_identity.documents_storage_blob_identity.name
  scope      = azurerm_user_assigned_identity.documents_storage_blob_identity.id
  lock_level = "CanNotDelete"
  notes      = "Lock for the Documents Storage Blob Managed Identity"
}

resource "azurerm_role_assignment" "documents_storage_blob_identity_role_assignment" {
  scope                = data.azurerm_storage_account.documents_storage.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.documents_storage_blob_identity.principal_id
}

###############################################################################
# Web Storage Blob Managed Identity
###############################################################################

resource "azurerm_user_assigned_identity" "web_storage_blob_identity" {
  name                = "${var.prefix}-${var.env_short}-${var.domain}-web-storage-blob-managed-identity"
  location            = var.location
  resource_group_name = azurerm_resource_group.user_managed_identity_rg.name
  tags                = var.tags
}

resource "azurerm_management_lock" "web_storage_blob_identity_lock" {
  name       = azurerm_user_assigned_identity.web_storage_blob_identity.name
  scope      = azurerm_user_assigned_identity.web_storage_blob_identity.id
  lock_level = "CanNotDelete"
  notes      = "Lock for the Web Storage Blob Managed Identity"
}

resource "azurerm_role_assignment" "web_storage_blob_identity_role_assignment" {
  scope                = data.azurerm_storage_account.web_storage.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.web_storage_blob_identity.principal_id
}
