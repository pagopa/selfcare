variable "resource_group_name" {
  type        = string
  description = "The name of the resource group."
}

variable "account_name" {
  type        = string
  description = "The name of the CosmosDB account."
}

variable "databases" {
  type = list(object({
    name           = string
    throughput     = optional(number, 1000)
    max_throughput = optional(number) # Se valorizzato, abilita l'autoscale
  }))
  default     = []
  description = "Configuration for MongoDB databases"

  validation {
    condition     = length(var.databases) == length(distinct([for db in var.databases : db.name]))
    error_message = "Each database name in var.databases must be unique."
  }
}

variable "collections" {
  type = list(object({
    database_name       = string
    name                = string
    shard_key           = optional(string, "_id")
    indexes             = list(object({ keys = list(string), unique = bool }))
    lock_enable         = optional(bool, true)
    default_ttl_seconds = optional(number)
    throughput          = optional(number)
    max_throughput      = optional(number)
  }))
  default     = []
  description = "Configuration for MongoDB collections. Each entry is an independent action."

  validation {
    condition = length(var.collections) == length(
      distinct([for coll in var.collections : "${coll.database_name}.${coll.name}"])
    )
    error_message = "Each collection (database_name + name) must be unique."
  }
}
