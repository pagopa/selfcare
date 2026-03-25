variable "app_name" {
  type        = string
  description = "App name"
}

variable "cidr_subnet" {
  type        = list(string)
  description = "Application gateway address space."
}

# variable "collection_name" {
#   type        = string
#   description = "Cosmos db collection name to be indexed on ai search"
# }
#
# variable "cosmosdb_prefix" {
#   type        = string
#   description = "Cosmos db account name prefix"
# }

# variable "database_name" {
#   type        = string
#   description = "Database to be indexed on ai search"
# }

variable "domain" {
  type        = string
  description = "Domain"
}

variable "env_short" {
  description = "Environment short name"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

# variable "instance_number" {
#   type        = string
#   description = "The istance number to create"
# }

variable "location" {
  type    = string
  default = "westeurope"
}

variable "key_vault_resource_group_name" {
  type        = string
  description = "Name of Key Vault resource group"
}

variable "key_vault_name" {
  type        = string
  description = "Name of Key Vault"
}

# variable "key_vault_cosmosdb_key" {
#   type        = string
#   description = "Name of Key Vault"
# }

variable "prefix" {
  description = "Domain prefix"
  type        = string
  default     = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "public_network_access_enabled" {
  type    = bool
  default = false
}

variable "srch_private_endpoint_enabled" {
  type    = bool
  default = true
}

variable "project" {
  type        = string
  description = "Selfcare prefix and short environment"
}

# variable "virtual_network_name" {
#   type        = string
#   description = "Name of the resource where resources will be created"
# }

# variable "suffix_increment" {
#   type        = string
#   description = "Suffix increment Container App Environment name"
#   default     = ""
# }

# variable "resource_group_name" {
#   type        = string
#   description = "Resource group"
# }

variable "sku" {
  type        = string
  description = "SKU of the resource: free, basic, standard, standard2, standard3, storage_optimized_l1, storage_optimized_l2"
}

variable "tags" {
  type = map(any)
}
