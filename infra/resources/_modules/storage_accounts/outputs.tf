output "storage_account" {
  value = {
    id                  = module.storage_account.id
    name                = module.storage_account.name
    resource_group_name = module.storage_account.resource_group_name
    primary_web_host    = module.storage_account.primary_web_host
  }
}