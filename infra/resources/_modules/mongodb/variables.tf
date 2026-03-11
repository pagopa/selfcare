variable "resource_group_name" {
  type        = string
  description = "The name of the resource group."
}

variable "account_name" {
  type        = string
  description = "The name of the CosmosDB account."
}

variable "databases" {
  type = map(object({
    throughput     = optional(number, 1000)
    max_throughput = optional(number) # Se valorizzato, abilita l'autoscale
    collections = map(object({
      shard_key = optional(string, "_id")
      indexes = list(object({
        keys   = list(string)
        unique = bool
      }))
      lock_enable         = optional(bool, true)
      default_ttl_seconds = optional(number)
      throughput          = optional(number)
      max_throughput      = optional(number)
    }))
  }))
  default     = {}
  description = "Configuration for databases and their collections"
}
