output "mongodb_id" {
  value = azurerm_cosmosdb_mongo_database.this.id
}

output "mongodb_name" {
  value = azurerm_cosmosdb_mongo_database.this.name
}

output "collection_onboardings_name" {
  value = module.collection_onboardings.name
}

output "collection_tokens_name" {
  value = module.collection_tokens.name
}
