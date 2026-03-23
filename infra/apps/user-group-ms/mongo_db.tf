resource "azurerm_cosmosdb_mongo_database" "selc_user_group" {
  name                = "selcUserGroup"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name

  dynamic "autoscale_settings" {
    for_each = var.autoscale_settings
    content {
      max_throughput = autoscale_settings.value.max_throughput
    }
  }
}

resource "azurerm_management_lock" "mongodb_selc_user_group" {
  name       = "mongodb-selc-user-group-lock"
  scope      = azurerm_cosmosdb_mongo_database.selc_user_group.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

module "mongodb_collection_user-groups" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_mongodb_collection?ref=v7.26.5"

  name                = "UserGroups"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_user_group.name

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["institutionId"]
      unique = false
    },
    {
      keys   = ["parentInstitutionId"]
      unique = false
    },
    {
      keys   = ["productId"]
      unique = false
    },
    {
      keys   = ["members"]
      unique = false
    },
    {
      keys   = ["name"]
      unique = false
    }
  ]

  lock_enable = true
}
