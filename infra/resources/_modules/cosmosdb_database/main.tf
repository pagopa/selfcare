data "azurerm_cosmosdb_account" "cosmosdb" {
  name                = var.cosmosdb_mongo_account_name
  resource_group_name = var.resource_group_name
}

resource "azurerm_cosmosdb_mongo_database" "this" {
  name                = var.database_name
  resource_group_name = data.azurerm_cosmosdb_account.cosmosdb.resource_group_name
  account_name        = data.azurerm_cosmosdb_account.cosmosdb.name
}

# Lock
resource "azurerm_management_lock" "database" {
  name       = "mongodb-${azurerm_cosmosdb_mongo_database.this.name}-lock"
  scope      = azurerm_cosmosdb_mongo_database.this.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

