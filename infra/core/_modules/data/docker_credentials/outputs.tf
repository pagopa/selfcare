output "hub_docker_user" {
  value     = data.azurerm_key_vault_secret.hub_docker_user.value
  sensitive = true
}

output "hub_docker_pwd" {
  value     = data.azurerm_key_vault_secret.hub_docker_pwd.value
  sensitive = true
}

