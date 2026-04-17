module "github_runner" {
  source = "../_modules/github_runner"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  key_vault = {
    name                = "${local.project}-kv"
    resource_group_name = "${local.project}-sec-rg"
    pat_secret_name     = "github-runner-pat"
  }

  networking = {
    vnet_resource_group_name = "${local.project}-vnet-rg"
    vnet_name                = "${local.project}-vnet"
    subnet_cidr_block        = "10.1.146.0/23"
  }

  law = {
    name                = "${local.project}-law"
    resource_group_name = "${local.project}-monitor-rg"
  }
}

resource "azurerm_key_vault_access_policy" "container_app_environment" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.github_runner.user_assigned_identity.principal_id

  secret_permissions = ["Get", "List"]
}

resource "azurerm_key_vault_access_policy" "container_app_environment_pnpg" {
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.github_runner.user_assigned_identity.principal_id

  secret_permissions = ["Get", "List"]
}