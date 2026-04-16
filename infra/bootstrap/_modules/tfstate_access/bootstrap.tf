data "azurerm_storage_account" "tfstate" {
  name                = var.storage_account_name
  resource_group_name = var.storage_resource_group_name
}

# Container per lo stato Terraform
data "azurerm_storage_container" "tfstate" {
  name                 = var.storage_container_name
  storage_account_name = data.azurerm_storage_account.tfstate.name
}

data "azuread_group" "adgroup_admin" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-admin"
}

data "azuread_group" "adgroup_developers" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-developers"
}

data "azuread_group" "adgroup_externals" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-externals"
}

# Assegna il ruolo al service principal corrente
resource "azurerm_role_assignment" "storage_blob_contributor_developers" {
  scope                = data.azurerm_storage_account.tfstate.id
  role_definition_name = var.storage_role_name
  principal_id         = data.azuread_group.adgroup_developers.object_id
}

resource "azurerm_role_assignment" "storage_blob_contributor_admin" {
  scope                = data.azurerm_storage_account.tfstate.id
  role_definition_name = var.storage_role_name
  principal_id         = data.azuread_group.adgroup_admin.object_id
}

resource "azurerm_role_assignment" "storage_blob_contributor_externals" {
  count = var.env_short != "p" ? 1 : 0

  scope                = data.azurerm_storage_account.tfstate.id
  role_definition_name = var.storage_role_name
  principal_id         = data.azuread_group.adgroup_externals.object_id
}
