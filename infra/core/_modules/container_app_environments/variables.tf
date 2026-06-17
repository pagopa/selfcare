variable "project" {
  type        = string
  description = "SelfCare prefix and short environment"
}

variable "location" {
  type        = string
  description = "Azure region"
}

variable "tags" {
  type        = map(any)
  description = "Resource tags"
}

variable "resource_group_name" {
  type        = string
  description = "Name of the resource group where resources will be created"
}
variable "enable_log" {
  type        = bool
  description = "Enable or disable logging"
  default     = true
}

variable "subnet_id" {
  type        = string
  description = "Id of the subnet to use for Container App Environment"
}

# variable "pnpg_subnet_id" {
#   type        = string
#   description = "Id of the subnet to use for PNPG Container App Environment"
# }

variable "zone_redundant" {
  type        = bool
  description = "Enable or not the zone redundancy"
}

variable "cae_name" {
  type        = string
  description = "Name of Container App env"
}

# variable "pnpg_cae_name" {
#   type        = string
#   description = "Name of Container App env"
# }

variable "workload_profiles" {
  description = "Workload profiles"
  type = list(object({
    name                  = string
    workload_profile_type = string
    minimum_count         = number
    maximum_count         = number
  }))
  default = [
    {
      name                  = "Consumption"
      workload_profile_type = "Consumption"
      minimum_count         = 0
      maximum_count         = 1
    }
  ]
}

variable "infrastructure_resource_group_name" {
  type        = string
  description = "Name of the platform-managed resource group created for the Managed Environment to host infrastructure resources. Changing this forces a new resource to be created."
  default     = null
}

# variable "pnpg_workload_profiles" {
#   description = "PNPG workload profiles"
#   type = list(object({
#     name                  = string
#     workload_profile_type = string
#     minimum_count         = number
#     maximum_count         = number
#   }))
#   default = [
#     {
#       name                  = "Consumption"
#       workload_profile_type = "Consumption"
#       minimum_count         = 0
#       maximum_count         = 1
#     }
#   ]
# }
