output "mongodb_ids" {
  value = { for k, v in azurerm_cosmosdb_mongo_database.this : k => v.id }
}

output "mongodb_names" {
  value = { for k, v in azurerm_cosmosdb_mongo_database.this : k => v.name }
}

output "collection_names" {
  value = { for k, v in module.collections : k => v.name }
}
