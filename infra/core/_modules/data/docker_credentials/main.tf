data "azurerm_key_vault_secret" "hub_docker_user" {
  name         = "hub-docker-user"
  key_vault_id = var.key_vault_id
}

data "azurerm_key_vault_secret" "hub_docker_pwd" {
  name         = "hub-docker-pwd"
  key_vault_id = var.key_vault_id
}
