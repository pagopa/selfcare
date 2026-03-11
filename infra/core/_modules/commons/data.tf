resource "azurerm_resource_group" "data" {
  name     = "${local.project}-data-rg"
  location = var.location

  tags = var.tags
}
