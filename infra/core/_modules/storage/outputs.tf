output "data_rg_name" {
  value = azurerm_resource_group.data.name
}

output "tfstate_storage_account_name" {
  value = data.azurerm_storage_account.tfstate.name
}
