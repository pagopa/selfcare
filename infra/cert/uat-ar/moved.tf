moved {
  from = module.jwt
  to   = module.cert.module.jwt
}

moved {
  from = module.jwt_exchange
  to   = module.cert.module.jwt_exchange
}

moved {
  from = null_resource.upload_jwks
  to   = module.cert.null_resource.upload_jwks
}

moved {
  from = random_password.encryption_key
  to   = module.cert.random_password.encryption_key
}

moved {
  from = random_password.encryption_iv
  to   = module.cert.random_password.encryption_iv
}

moved {
  from = azurerm_key_vault_secret.encryption_iv_secret
  to   = module.cert.azurerm_key_vault_secret.encryption_iv_secret
}

moved {
  from = azurerm_key_vault_secret.encryption_key_secret
  to   = module.cert.azurerm_key_vault_secret.encryption_key_secret
}

moved {
  from = data.azurerm_key_vault.key_vault
  to   = module.cert.data.azurerm_key_vault.key_vault
}

moved {
  from = data.azurerm_key_vault_secret.web_storage_access_key
  to   = module.cert.data.azurerm_key_vault_secret.web_storage_access_key
}

moved {
  from = data.azurerm_resource_group.checkout_fe_rg
  to   = module.cert.data.azurerm_resource_group.checkout_fe_rg
}
