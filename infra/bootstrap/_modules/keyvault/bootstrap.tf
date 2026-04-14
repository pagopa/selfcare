

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

resource "azurerm_key_vault_secret" "adgroup_admin" {
  name         = "${var.prefix}-${var.env_short}-adgroup-admin"
  content_type = "text/plain"

  value_wo         = data.azuread_group.adgroup_admin.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}

resource "azurerm_key_vault_secret" "adgroup_developers" {
  name         = "${var.prefix}-${var.env_short}-adgroup-developers"
  content_type = "text/plain"

  value_wo         = data.azuread_group.adgroup_developers.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}

resource "azurerm_key_vault_secret" "adgroup_externals" {
  name         = "${var.prefix}-${var.env_short}-adgroup-externals"
  content_type = "text/plain"

  value_wo         = data.azuread_group.adgroup_externals.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}

resource "azurerm_key_vault_secret" "adgroup_security" {
  name         = "${var.prefix}-${var.env_short}-adgroup-security"
  content_type = "text/plain"

  value_wo         = data.azuread_group.adgroup_security.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}

data "azurerm_subscription" "current" {}

# azure devops policy
data "azuread_service_principal" "iac_principal" {
  display_name = "pagopaspa-selfcare-iac-projects-${data.azurerm_subscription.current.subscription_id}"
}

# azure devops policy
data "azuread_service_principal" "app_projects_principal" {
  # display_name = format("pagopaspa-selfcare-platform-app-projects-%s", var.subscription_id)
  client_id = "857f30ee-6e15-4b50-a9f3-cfd2ab2d3a29"
}


resource "azurerm_key_vault_secret" "iac_principal" {
  name         = "pagopaspa-selfcare-iac-projects"
  content_type = "text/plain"

  value_wo         = data.azuread_service_principal.iac_principal.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}

resource "azurerm_key_vault_secret" "app_projects_principal" {
  name         = "pagopaspa-selfcare-platform-app-projects"
  content_type = "text/plain"

  value_wo         = data.azuread_service_principal.app_projects_principal.object_id
  value_wo_version = 1
  key_vault_id     = var.key_vault_id
}
