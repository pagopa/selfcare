moved {
  from = azurerm_cosmosdb_mongo_database.this
  to   = azurerm_cosmosdb_mongo_database.this["selcOnboarding"]
}

moved {
  from = module.collection_onboardings.azurerm_cosmosdb_mongo_collection.this
  to   = azurerm_cosmosdb_mongo_collection.this["selcOnboarding.onboardings"]
}

moved {
  from = module.collection_tokens.azurerm_cosmosdb_mongo_collection.this
  to   = azurerm_cosmosdb_mongo_collection.this["selcOnboarding.tokens"]
}
