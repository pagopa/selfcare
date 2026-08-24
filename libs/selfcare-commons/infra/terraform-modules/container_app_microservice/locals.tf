locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  project     = "selc-${var.env_short}"

  resource_group_name                     = var.resource_group_name
  vnet_name                               = "${local.project}-vnet-rg"
  key_vault_resource_group_name           = "${local.project}${local.pnpg_suffix}-sec-rg"
  key_vault_name                          = "${local.project}${local.pnpg_suffix}-kv"
  container_app_environment_name          = var.container_app_environment_name
  container_name                          = "${local.project}${local.pnpg_suffix}-${var.container_app_name}"
  app_name                                = "${local.container_name}-ca"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"

  secrets = [for secret in var.secrets_names :
    {
      identity               = var.user_assigned_identity_id
      name                   = secret
      # key_vault_secret_name  = "@Microsoft.KeyVault(VaultName=${data.azurerm_key_vault.key_vault.name};SecretName=${secret})"
      key_vault_secret_name  = "https://${data.azurerm_key_vault.key_vault.name}.vault.azure.net/secrets/${secret}"

      # keyVaultId  = data.azurerm_key_vault_secret.keyvault_secret[secret].id
  }]

  secrets_env = [for env, secret in var.secrets_names :
    {
      name      = env
      secretRef = secret
  }]

}
