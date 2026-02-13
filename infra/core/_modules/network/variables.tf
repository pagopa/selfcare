variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "location_short" {
  type = string
}

variable "location_pair" {
  type = string
}

variable "location_pair_short" {
  type = string
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "cidr_vnet" {
  type        = list(string)
  description = "Virtual network address space."
}

variable "cidr_pair_vnet" {
  type        = list(string)
  description = "Virtual network pair address space."
}

variable "cidr_aks_platform_vnet" {
  type        = list(string)
  description = "VNet for AKS platform."
}

variable "cidr_subnet_private_endpoints" {
  type        = list(string)
  description = "Private endpoints address space."
}

variable "private_endpoint_network_policies" {
  type    = string
  default = "Enabled"
}

variable "ddos_protection_plan" {
  type = object({
    id     = string
    enable = bool
  })
  default = null
}

variable "aks_platform_env" {
  type        = string
  description = "The env name used into AKS platform folder."
}
