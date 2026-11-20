data "azurerm_key_vault" "project" {
  name = provider::dx::resource_name(merge(local.key_vault_naming, {
    resource_type = "key_vault"
  }))
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_log_analytics_workspace" "law" {
  name                = var.law.name
  resource_group_name = var.law.resource_group_name
}
