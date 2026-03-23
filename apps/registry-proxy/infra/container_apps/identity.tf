resource "azurerm_role_assignment" "blob_state_access" {
  count = var.is_pnpg ? 0 : 1
  scope                = try(data.azurerm_storage_account.existing_logs_storage[0].id, null)
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = try(data.azurerm_user_assigned_identity.cae_identity.principal_id, null)
}