output "storage_account" {
  value = {
    id                        = module.storage_account.id
    name                      = module.storage_account.name
    resource_group_name       = module.storage_account.resource_group_name
    primary_web_host          = module.storage_account.primary_web_host
    primary_connection_string = module.storage_account.primary_connection_string
  }
}

output "storage_container_name" {
  value = azurerm_storage_container.selc_documents_blob.name
}