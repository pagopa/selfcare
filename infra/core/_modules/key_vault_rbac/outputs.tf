output "key_vault" {
  value = {
    id                  = azurerm_key_vault.this.id
    name                = azurerm_key_vault.this.name
    resource_group_name = azurerm_key_vault.this.resource_group_name
    vault_uri           = azurerm_key_vault.this.vault_uri
  }
  description = "Project RBAC Key Vault; secret values and consumers' reader roles are managed separately"
}
