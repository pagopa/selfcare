resource "azurerm_resource_group" "identity_rg" {
  name     = "${local.project}-identity-rg"
  location = local.location
}

resource "azurerm_resource_group" "synthetic_monitoring" {
  name     = "${local.project}-synthetic-monitoring-rg"
  location = local.location
  tags     = local.tags
}
