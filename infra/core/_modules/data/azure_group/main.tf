locals {
  project = "${var.prefix}-${var.env_short}"
}

data "azurerm_subscription" "current" {}
data "azurerm_client_config" "current" {}

