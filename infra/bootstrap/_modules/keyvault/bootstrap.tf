

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


