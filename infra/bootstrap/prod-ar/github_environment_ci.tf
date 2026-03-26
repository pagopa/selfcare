module "github_environment_ci" {
  source = "../_modules/github_repository_environment"

  env                    = local.env
  env_suffix             = "ci"
  repository             = local.github.repository
  branch_policy_enabled  = local.github.ci_branch_policy_enabled
  repository_environment = local.github_repository_environment_ci
  env_secrets            = local.env_ci_secrets
  key_vault_id           = data.azurerm_key_vault.key_vault.id
  kv_secrets = {
    "STORAGE_CHECKOUT_ACCOUNT_KEY"        = "web-storage-access-key"
    "STORAGE_CONTRACTS_ACCOUNT_KEY"       = "contracts-storage-access-key"
    "STORAGE_CONNECTION_STRING_DOCUMENTS" = "documents-storage-connection-string"
  }
}
