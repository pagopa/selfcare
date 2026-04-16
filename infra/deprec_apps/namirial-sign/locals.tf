locals {
  project                                 = "selc-${var.env_short}"
  container_app_environment_name          = "${local.project}-${var.cae_name}"
  ca_resource_group_name                  = "${local.project}-container-app${var.suffix_increment}-rg"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
  vnet_name                               = "${local.project}-vnet-rg"

  secrets = [for secret in var.secrets_names :
    {
      identity    = "system"
      name        = "${secret}"
      keyVaultUrl = data.azurerm_key_vault_secret.keyvault_secret["${secret}"].id
  }]

  secrets_env = [for env, secret in var.secrets_names :
    {
      name      = env
      secretRef = secret
  }]
}