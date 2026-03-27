module "identity_cd" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = var.domain

  identity_role = "cd"

  github_federations = var.cd_github_federations

  cd_rbac_roles = {
    subscription_roles = var.environment_cd_roles.subscription
    resource_groups    = var.environment_cd_roles.resource_groups
  }

  tags = var.tags
}

module "identity_cd_ms" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = "ms"

  identity_role = "cd"

  github_federations = var.cd_github_federations_ms

  cd_rbac_roles = {
    subscription_roles = concat(var.environment_cd_roles_ms.subscription, ["${var.app} ${var.env} ContainerApp Jobs Writer"])
    resource_groups    = var.environment_cd_roles_ms.resource_groups
  }

  tags = var.tags

  depends_on = [
    azurerm_role_definition.container_apps_jobs_writer
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_identity_cd" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_cd_ms.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
    "Set"
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_pnpg_identity_cd" {
  key_vault_id = var.key_vault_pnpg_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_cd_ms.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
    "Set"
  ]
}

module "identity_cd_fe" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = "fe"

  identity_role = "cd"

  github_federations = var.cd_github_federations_fe

  cd_rbac_roles = {
    subscription_roles = var.environment_cd_roles_ms.subscription
    resource_groups    = var.environment_cd_roles_ms.resource_groups
  }

  tags = var.tags
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_identity_fe_cd" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_cd_fe.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_pnpg_identity_fe_cd" {
  key_vault_id = var.key_vault_pnpg_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_cd_fe.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_role_definition" "container_apps_jobs_writer" {
  name        = "${var.app} ${var.env} ContainerApp Jobs Writer"
  scope       = var.subscription_id
  description = "Custom role used to write container apps jobs execution properties"

  permissions {
    actions = [
      "Microsoft.Authorization/roleDefinitions/write"
    ]
  }

  assignable_scopes = [
    var.subscription_id
  ]
}
