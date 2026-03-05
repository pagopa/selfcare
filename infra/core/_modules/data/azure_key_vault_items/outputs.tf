# Certificate outputs for appgateway
output "app_gw_platform_cert_secret_id" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.secret_id
}

output "app_gw_platform_cert_version" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.version
}

output "app_gw_platform_certificate_secret_id" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.secret_id
}

output "api_pnpg_cert_secret_id" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.secret_id
}

output "api_pnpg_cert_version" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.version
}

output "api_pnpg_selfcare_certificate_secret_id" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.secret_id
}

output "apim_publisher_email" {
  value     = data.azurerm_key_vault_secret.apim_publisher_email.value
  sensitive = true
}

output "sec_workspace_id" {
  value     = try(data.azurerm_key_vault_secret.sec_workspace_id[0].value, null)
  sensitive = true
}

output "sec_storage_id" {
  value     = try(data.azurerm_key_vault_secret.sec_storage_id[0].value, null)
  sensitive = true
}
