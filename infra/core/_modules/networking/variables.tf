variable "project" {
  type        = string
  description = "SelfCare prefix and short environment"
}

variable "tags" {
  type        = map(any)
  description = "Resource tags"
}

variable "cidr_subnet_cae" {
  type        = string
  description = "CIDR block for ContainerAppEnvironment subnet"
}

variable "cidr_subnet_main" {
  type        = string
  description = "CIDR block for main subnet"
  default     = null
}
variable "core_vnet" {
  type        = bool
  description = "True if the module define the core vnet, true for AR"
  default     = true
}

# variable "cidr_subnet_pnpg_cae" {
#   type        = string
#   description = "CIDR block for PNPG ContainerAppEnvironment subnet"
# }

variable "container_app_name_snet" {
  type        = string
  description = "Name of container app subnet"
}

# variable "pnpg_container_app_name_snet" {
#   type        = string
#   description = "Name of pnpg subnet"
# }

# variable "pnpg_delegation" {
#   description = "PNPG subnet delegation"
#   type = list(object({
#     name                       = string
#     service_delegation_name    = string
#     service_delegation_actions = list(string)
#   }))
#   default = [
#     {
#       name                       = "Microsoft.App/environments"
#       service_delegation_name    = "Microsoft.App/environments"
#       service_delegation_actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
#     }
#   ]
# }

variable "delegation" {
  description = "Container app subnet delegation"
  type = list(object({
    name                       = string
    service_delegation_name    = string
    service_delegation_actions = list(string)
  }))
  default = [
    {
      name                       = "Microsoft.App/environments"
      service_delegation_name    = "Microsoft.App/environments"
      service_delegation_actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  ]
}
