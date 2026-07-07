resource "azurerm_storage_container" "selc_user_attachments_blob" {
  name                  = "${var.prefix}-${var.env_short}-${var.app_name}-blob"
  storage_account_id    = module.storage_account.id
  container_access_type = "private"
}

resource "azurerm_management_lock" "selc_user_attachments_blob_management_lock" {
  name       = azurerm_storage_container.selc_user_attachments_blob.name
  scope      = azurerm_storage_container.selc_user_attachments_blob.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

