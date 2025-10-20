resource "azurerm_cosmosdb_mongo_database" "selc_iam" {
  count               = var.is_pnpg ? 0 : 1
  name                = "selcIam"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}

resource "azurerm_management_lock" "mongodb_selc_iam" {
  count      = var.is_pnpg ? 0 : 1
  name       = "mongodb-selc-iam-lock"
  scope      = azurerm_cosmosdb_mongo_database.selc_iam[0].id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

module "mongodb_collection_iam_user" {
  count  = var.is_pnpg ? 0 : 1
  source = "github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_mongodb_collection?ref=v6.6.0"

  name                = "userClaims"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_iam[0].name

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["email"]
      unique = false
    }
  ]

  lock_enable = true
}