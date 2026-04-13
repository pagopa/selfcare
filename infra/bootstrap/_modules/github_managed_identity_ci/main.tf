module "identity_ci" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = var.domain

  identity_role = "ci"

  github_federations = var.ci_github_federations

  ci_rbac_roles = {
    subscription_roles = var.environment_ci_roles.subscription
    resource_groups    = var.environment_ci_roles.resource_groups
  }

  tags = var.tags
}

module "identity_ci_ms" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = "ms"

  identity_role = "ci"

  github_federations = var.ci_github_federations_ms

  ci_rbac_roles = {
    subscription_roles = concat(var.environment_ci_roles_ms.subscription, ["${var.app} ${var.env} ContainerApp Jobs Reader", "${var.app} ${var.env} APIM Integration Reader"])
    resource_groups = merge(var.environment_ci_roles_ms.resource_groups,
      {
        "selc-${var.env_short}-checkout-fe-rg" = ["Storage Blob Data Contributor", "Storage Account Key Operator Service Role", "CDN Endpoint Contributor"]
        "selc-${var.env_short}-weu-ar-srch-rg" = ["Search Service Contributor"]
    })
  }

  tags = var.tags

  depends_on = [
    azurerm_role_definition.container_apps_jobs_reader,
    azurerm_role_definition.apim_integration_reader
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_selfcare_identity_ci" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_ci.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_identity_ci" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_ci_ms.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_pnpg_identity_ci" {
  key_vault_id = var.key_vault_pnpg_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_ci_ms.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]
}

module "identity_ci_fe" {
  source = "github.com/pagopa/terraform-azurerm-v4//github_federated_identity?ref=v9.6.1"

  prefix    = var.prefix
  env_short = var.env_short
  domain    = "fe"

  identity_role = "ci"

  github_federations = var.ci_github_federations_fe

  ci_rbac_roles = {
    subscription_roles = concat(var.environment_ci_roles_ms.subscription, ["${var.app} ${var.env} ContainerApp Jobs Reader", "${var.app} ${var.env} APIM Integration Reader"])
    resource_groups = merge(var.environment_ci_roles_ms.resource_groups,
      {
        "selc-${var.env_short}-checkout-fe-rg" = ["Storage Blob Data Contributor", "Storage Account Key Operator Service Role", "CDN Endpoint Contributor"]
    })
  }

  tags = var.tags

  depends_on = [
    azurerm_role_definition.container_apps_jobs_reader,
    azurerm_role_definition.apim_integration_reader
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_identity_fe_ci" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_ci_fe.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]

  certificate_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_key_vault_access_policy" "key_vault_access_policy_pnpg_identity_fe_ci" {
  key_vault_id = var.key_vault_pnpg_id
  tenant_id    = var.tenant_id
  object_id    = module.identity_ci_fe.identity_principal_id

  secret_permissions = [
    "Get",
    "List",
  ]
}

resource "azurerm_role_definition" "container_apps_jobs_reader" {
  name        = "${var.app} ${var.env} ContainerApp Jobs Reader"
  scope       = var.subscription_id
  description = "Custom role used to read container apps jobs execution properties"

  permissions {
    actions = [
      "microsoft.app/jobs/read",
      "microsoft.app/jobs/listsecrets/action",
      "microsoft.app/jobs/detectors/read",
      "microsoft.app/jobs/execution/read",
      "microsoft.app/jobs/executions/read",
      "microsoft.app/containerApps/read",
      "microsoft.app/containerApps/listSecrets/action"
    ]
  }

  assignable_scopes = [
    var.subscription_id
  ]
}

resource "azurerm_role_definition" "apim_integration_reader" {
  name        = "${var.app} ${var.env} APIM Integration Reader"
  scope       = var.subscription_id
  description = "Custom role used to read APIM integration secrets"

  permissions {
    actions     = ["Microsoft.ApiManagement/service/portalSettings/listSecrets/action", "Microsoft.ApiManagement/service/tenant/listSecrets/action"]
    not_actions = []
  }

  assignable_scopes = [
    var.subscription_id,
  ]
}
