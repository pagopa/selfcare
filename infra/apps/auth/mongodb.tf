resource "azurerm_cosmosdb_mongo_database" "selc_auth" {
  count               = var.is_pnpg ? 0 : 1
  name                = "selcAuth"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}

resource "azurerm_management_lock" "mongodb_selc_auth" {
  count      = var.is_pnpg ? 0 : 1
  name       = "mongodb-selc-auth-lock"
  scope      = azurerm_cosmosdb_mongo_database.selc_auth[0].id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

module "mongodb_collection_otp_flows" {
  count  = var.is_pnpg ? 0 : 1
  source = "github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_mongodb_collection?ref=v8.5.3"

  name                = "otpFlows"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_auth[0].name

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["uuid"]
      unique = true
    },
    {
      keys   = ["userId"]
      unique = false
    },
    {
      keys   = ["expiresAt"]
      unique = false
    },
    {
      keys   = ["status"]
      unique = false
    },
    {
      keys   = ["userId", "createdAt"]
      unique = false
    },
    {
      keys   = ["createdAt"]
      unique = false
    }
  ]

  lock_enable = true
}