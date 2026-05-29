module "identity_ci" {
  source = "../_modules/github_managed_identity_ci"

  prefix    = local.prefix
  env_short = local.env_short
  domain    = local.domain
  app       = local.app
  env       = local.env
  tags      = local.tags

  key_vault_id      = data.azurerm_key_vault.key_vault.id
  key_vault_pnpg_id = data.azurerm_key_vault.key_vault_pnpg.id
  tenant_id         = data.azurerm_client_config.current.tenant_id
  subscription_id   = data.azurerm_subscription.current.id

  ci_github_federations    = local.ci_github_federations
  ci_github_federations_fe = local.ci_github_federations_fe

  environment_ci_roles    = local.environment_ci_roles
  environment_ci_roles_ms = local.environment_ci_roles_ms

  depends_on = [
    azurerm_resource_group.identity_rg,
    module.identity_cd
  ]
}

module "identity_opex_ci" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = local.prefix
  env_short = local.env_short
  domain    = "opex"

  identity_role = "ci"

  github_federations = [{
    repository = "selfcare"
    subject    = "opex-${local.env}-ci"
  }]

  ci_rbac_roles = {
    subscription_roles = local.environment_ci_roles.subscription
    resource_groups = merge(local.environment_ci_roles.resource_groups,
      {

    })
  }

  tags = local.tags
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_opex_identity_ci" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.identity_opex_ci.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

module "identity_opex_cd" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = local.prefix
  env_short = local.env_short
  domain    = "opex"

  identity_role = "cd"

  github_federations = [{
    repository = "selfcare"
    subject    = "opex-${local.env}-cd"
  }]

  ci_rbac_roles = {
    subscription_roles = local.environment_cd_roles.subscription
    resource_groups = merge(local.environment_cd_roles.resource_groups,
      {

    })
  }

  tags = local.tags
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_opex_identity_cd" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.identity_opex_cd.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}
