
resource "github_repository_environment" "repo_environment" {
  repository  = data.github_repository.repo.name
  environment = "${module.local.config.env}-ci"
}

# resource "github_actions_environment_secret" "integration_environment" {
#   repository  = data.github_repository.repo.name
#   environment = github_repository_environment.repo_environment.environment
#   secret_name = "integration_environment"
#   //PNPG secret_name="integration_environment${local.pnpg_suffix}"
#   value = base64encode(templatefile("../Selfcare-external-Integration.postman_environment.json",
#     {
#       env                  = module.local.config.env
#       apimKeyPN            = data.azurerm_key_vault_secret.apim_product_pn_sk.value
#       apimKeyDataVaultPNPG = data.azurerm_key_vault_secret.apim_product_pnpg_sk.value
#   }))
# }

resource "github_actions_environment_secret" "integration_environment_bruno" {
  repository  = data.github_repository.repo.name
  environment = github_repository_environment.repo_environment.environment
  secret_name = "integration_environment_bruno"
  //PNPG secret_name="integration_environment_bruno${local.pnpg_suffix}"
  value = base64encode(templatefile("../Selfcare-External-Integration-Environment.json",
    {
      env                  = module.local.config.env
      apimKeyPN            = data.azurerm_key_vault_secret.apim_product_pn_sk.value
      apimKeyDataVaultPNPG = data.azurerm_key_vault_secret.apim_product_pnpg_sk.value
      apimKeyInternal      = data.azurerm_key_vault_secret.apim_internal_sk.value
      apimKeySupport       = data.azurerm_key_vault_secret.apim_support_sk.value
  }))
}
