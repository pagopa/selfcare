data "azurerm_subscription" "current" {}
data "azurerm_client_config" "current" {}

# data "azurerm_api_management" "apim" {
#   name                = "${var.project}-apim-v2"
#   resource_group_name = "${var.project}-api-v2-rg"
# }

resource "azurerm_resource_group" "sec_rg" {
  name     = "${var.project}-sec-rg"
  location = var.location
  tags     = var.tags
}

module "key_vault" {
  source                     = "github.com/pagopa/terraform-azurerm-v4.git//key_vault?ref=v9.6.1"
  name                       = "${var.project}-kv"
  location                   = azurerm_resource_group.sec_rg.location
  resource_group_name        = azurerm_resource_group.sec_rg.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  tags                       = var.tags
  sku_name                   = var.sku_name
  soft_delete_retention_days = var.soft_delete_retention_days
}

# Azure AD Groups
data "azuread_group" "adgroup_admin" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-admin"
}

data "azuread_group" "adgroup_developers" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-developers"
}

data "azuread_group" "adgroup_externals" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-externals"
}

data "azuread_group" "adgroup_security" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-security"
}

resource "azurerm_key_vault_access_policy" "adgroup_admin_policy" {
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_group.adgroup_admin.object_id

  key_permissions         = ["Get", "List", "Update", "Create", "Import", "Delete"]
  secret_permissions      = ["Get", "List", "Set", "Delete", "Recover", "Backup", "Restore"]
  storage_permissions     = []
  certificate_permissions = ["Get", "List", "Update", "Create", "Import", "Delete", "Restore", "Purge", "Recover"]
}

resource "azurerm_key_vault_access_policy" "adgroup_developers_policy" {
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_group.adgroup_developers.object_id

  key_permissions         = var.env_short == "d" ? ["Get", "List", "Update", "Create", "Import", "Delete"] : ["Get", "List", "Update", "Create", "Import"]
  secret_permissions      = var.env_short == "d" ? ["Get", "List", "Set", "Delete", "Restore", "Recover"] : ["Get", "List", "Set"]
  storage_permissions     = []
  certificate_permissions = var.env_short == "d" ? ["Get", "List", "Update", "Create", "Import", "Delete", "Restore", "Purge", "Recover"] : ["Get", "List", "Update", "Create", "Import", "Restore", "Recover"]
}

resource "azurerm_key_vault_access_policy" "adgroup_externals_policy" {
  count        = var.env_short == "d" ? 1 : 0
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_group.adgroup_externals.object_id

  key_permissions         = ["Get", "List", "Update", "Create", "Import", "Delete"]
  secret_permissions      = ["Get", "List", "Set", "Delete"]
  storage_permissions     = []
  certificate_permissions = ["Get", "List", "Update", "Create", "Import", "Delete", "Restore", "Purge", "Recover"]
}

resource "azurerm_key_vault_access_policy" "adgroup_security_policy" {
  count        = var.env_short == "d" ? 1 : 0
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_group.adgroup_security.object_id

  key_permissions         = ["Get", "List", "Update", "Create", "Import", "Delete"]
  secret_permissions      = ["Get", "List", "Set", "Delete"]
  storage_permissions     = []
  certificate_permissions = ["Get", "List", "Update", "Create", "Import", "Delete", "Restore", "Purge", "Recover"]
}

# Azure DevOps SP
data "azuread_service_principal" "azdo_sp_tls_cert" {
  count        = var.azdo_sp_tls_cert_enabled ? 1 : 0
  display_name = "azdo-sp-${var.project}-tls-cert"
}

resource "azurerm_key_vault_access_policy" "azdo_sp_tls_cert" {
  count        = var.azdo_sp_tls_cert_enabled ? 1 : 0
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_service_principal.azdo_sp_tls_cert[0].object_id

  certificate_permissions = ["Get", "List", "Import"]
}

resource "azurerm_key_vault_access_policy" "azure_cdn_frontdoor_policy" {
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.azuread_service_principal_azure_cdn_frontdoor_id

  secret_permissions      = ["Get"]
  certificate_permissions = ["Get"]
}

# azure devops policy
data "azuread_service_principal" "iac_principal" {
  display_name = format("pagopaspa-selfcare-iac-projects-%s", data.azurerm_subscription.current.subscription_id)
}

resource "azurerm_key_vault_access_policy" "azdevops_iac_policy" {
  key_vault_id = module.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azuread_service_principal.iac_principal.object_id

  secret_permissions      = ["Get", "List", "Set", ]
  certificate_permissions = ["SetIssuers", "DeleteIssuers", "Purge", "List", "Get"]
  storage_permissions     = []
}

# Secrets query modules
module "secrets_selfcare_status_dev" {
  count  = var.env_short == "d" ? 1 : 0
  source = "github.com/pagopa/terraform-azurerm-v4.git//key_vault_secrets_query?ref=v9.6.1"

  resource_group = azurerm_resource_group.sec_rg.name
  key_vault_name = module.key_vault.name

  secrets = [
    "alert-selfcare-status-dev-email",
    "alert-selfcare-status-dev-slack",
  ]
}

module "secrets_selfcare_status_uat" {
  count  = var.env_short == "u" ? 1 : 0
  source = "github.com/pagopa/terraform-azurerm-v4.git//key_vault_secrets_query?ref=v9.6.1"

  resource_group = azurerm_resource_group.sec_rg.name
  key_vault_name = module.key_vault.name

  secrets = [
    "alert-selfcare-status-uat-email",
    "alert-selfcare-status-uat-slack",
  ]
}
