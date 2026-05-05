data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

data "azurerm_key_vault_secret" "apim_product_pn_sk" {
  name         = "apim-product-pn-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "apim_internal_sk" {
  name         = "apim-internal-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "apim_support_sk" {
  name         = "apim-support-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault" "key_vault_pnpg" {
  resource_group_name = var.key_vault_pnpg.resource_group_name
  name                = var.key_vault_pnpg.name
}

data "azurerm_key_vault_secret" "apim_product_pnpg_sk" {
  name         = "external-api-key"
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

data "github_repository" "repo" {
  full_name = "pagopa/selfcare-external-api-backend"
}

output "current_env" {
  value = local.env
}

resource "github_repository_environment" "repo_environment" {
  repository  = data.github_repository.repo.name
  environment = "${local.env}-ci"
}

resource "github_actions_environment_secret" "integration_environment" {
  repository  = data.github_repository.repo.name
  environment = github_repository_environment.repo_environment.environment
  secret_name = "integration_environment${local.pnpg_suffix}"
  plaintext_value = base64encode(templatefile("Selfcare-external-Integration.postman_environment.json",
    {
      env                  = local.env_url
      apimKeyPN            = data.azurerm_key_vault_secret.apim_product_pn_sk.value
      apimKeyDataVaultPNPG = data.azurerm_key_vault_secret.apim_product_pnpg_sk.value
    }))
}

resource "github_actions_environment_secret" "integration_environment_bruno" {
  repository  = data.github_repository.repo.name
  environment = github_repository_environment.repo_environment.environment
  secret_name = "integration_environment_bruno${local.pnpg_suffix}"
  plaintext_value = base64encode(templatefile("Selfcare-External-Integration-Environment.bru",
    {
      env                  = local.env_url
      apimKeyPN            = data.azurerm_key_vault_secret.apim_product_pn_sk.value
      apimKeyDataVaultPNPG = data.azurerm_key_vault_secret.apim_product_pnpg_sk.value
      apimKeyInternal      = data.azurerm_key_vault_secret.apim_internal_sk.value
      apimKeySupport      = data.azurerm_key_vault_secret.apim_support_sk.value
    }))
}
