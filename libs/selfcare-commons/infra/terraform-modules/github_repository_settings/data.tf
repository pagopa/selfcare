data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "github_team" "team_admins" {
  slug = "selfcare-admin"
}

data "azurerm_key_vault" "key_vault_dev" {
  provider = azurerm.dev

  name                = format(local.key_vault_name, local.dev_alias)
  resource_group_name = format(local.key_vault_resource_group_name, local.dev_alias)
}

data "azurerm_key_vault_secret" "key_vault_sonar" {
  name         = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault_dev.id
}

data "azurerm_key_vault_secret" "key_github_path_token" {
  name         = "github-path-token"
  key_vault_id = data.azurerm_key_vault.key_vault_dev.id
}

data "azurerm_user_assigned_identity" "identity_dev_ci" {
  provider = azurerm.dev

  name                = format(local.identity_ci_name, local.dev_alias)
  resource_group_name = format(local.identity_resource_group_name, local.dev_alias)
}

data "azurerm_user_assigned_identity" "identity_dev_cd" {
  provider = azurerm.dev

  name                = format(local.identity_cd_name, local.dev_alias)
  resource_group_name = format(local.identity_resource_group_name, local.dev_alias)
}

data "azurerm_user_assigned_identity" "identity_uat_ci" {
  provider = azurerm.uat

  name                = format(local.identity_ci_name, local.uat_alias)
  resource_group_name = format(local.identity_resource_group_name, local.uat_alias)
}

data "azurerm_user_assigned_identity" "identity_uat_cd" {
  provider            = azurerm.uat
  name                = format(local.identity_cd_name, local.uat_alias)
  resource_group_name = format(local.identity_resource_group_name, local.uat_alias)
}

data "azurerm_user_assigned_identity" "identity_prod_ci" {
  provider = azurerm.prod

  name                = format(local.identity_ci_name, local.prod_alias)
  resource_group_name = format(local.identity_resource_group_name, local.prod_alias)
}

data "azurerm_user_assigned_identity" "identity_prod_cd" {
  provider = azurerm.prod

  name                = format(local.identity_cd_name, local.prod_alias)
  resource_group_name = format(local.identity_resource_group_name, local.prod_alias)
}

data "azurerm_user_assigned_identity" "identity_dev_fe_ci" {
  provider = azurerm.dev

  name                = format(local.identity_ci_fe_name, local.dev_alias)
  resource_group_name = format(local.identity_resource_group_name, local.dev_alias)
}

data "azurerm_user_assigned_identity" "identity_dev_fe_cd" {
  provider = azurerm.dev

  name                = format(local.identity_cd_fe_name, local.dev_alias)
  resource_group_name = format(local.identity_resource_group_name, local.dev_alias)
}

data "azurerm_user_assigned_identity" "identity_uat_fe_ci" {
  provider = azurerm.uat

  name                = format(local.identity_ci_fe_name, local.uat_alias)
  resource_group_name = format(local.identity_resource_group_name, local.uat_alias)
}

data "azurerm_user_assigned_identity" "identity_uat_fe_cd" {
  provider            = azurerm.uat
  name                = format(local.identity_cd_fe_name, local.uat_alias)
  resource_group_name = format(local.identity_resource_group_name, local.uat_alias)
}

data "azurerm_user_assigned_identity" "identity_prod_fe_ci" {
  provider = azurerm.prod

  name                = format(local.identity_ci_fe_name, local.prod_alias)
  resource_group_name = format(local.identity_resource_group_name, local.prod_alias)
}

data "azurerm_user_assigned_identity" "identity_prod_fe_cd" {
  provider = azurerm.prod

  name                = format(local.identity_cd_fe_name, local.prod_alias)
  resource_group_name = format(local.identity_resource_group_name, local.prod_alias)
}
