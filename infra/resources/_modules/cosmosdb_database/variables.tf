# General Variables
variable "resource_group_name" {
  type        = string
  description = "The name of the resource group in which the Cosmos DB Mongo Collection is created. Changing this forces a new resource to be created."
}

variable "cosmosdb_mongo_account_name" {
  type        = string
  description = "The name of the Cosmos DB Mongo Account in which the Cosmos DB Mongo Database exists. Changing this forces a new resource to be created."
}

variable "database_name" {
  type        = string
  description = "The name of the Cosmos DB Mongo Database in which the Cosmos DB Mongo Collection is created. Changing this forces a new resource to be created."
}