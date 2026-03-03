resource "random_password" "encryption_key" {
  length           = 32
  special          = false

  keepers = {
    version = 1
  }
}

resource "random_password" "encryption_iv" {
  length           = 12
  special          = false

  keepers = {
    version = 1
  }
}

resource "azurerm_key_vault_secret" "encryption_iv_secret" {
  name         = "onboarding-data-encryption-iv"
  value        = random_password.encryption_iv.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "encryption_key_secret" {
  name         = "onboarding-data-encryption-key"
  value        = random_password.encryption_key.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}