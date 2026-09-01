variable "project" {
  type        = string
  description = "Environment project prefix, for example selc-d."
}

variable "location" {
  type        = string
  description = "Azure region where the storage resources are created."
}

variable "resource_group_name" {
  type        = string
  description = "Pre-existing resource group where the Storage Account is created."
}

variable "private_endpoint_subnet_id" {
  type        = string
  description = "Existing private endpoint subnet reused by the Table private endpoint."
}

variable "virtual_network_id" {
  type        = string
  description = "Virtual network linked to the Storage Table private DNS zone."
}

variable "virtual_network_name" {
  type        = string
  description = "Virtual network name used for the private DNS link."
}

variable "virtual_network_resource_group_name" {
  type        = string
  description = "Resource group containing the virtual network and private DNS zone."
}

variable "tags" {
  type        = map(string)
  description = "Tags applied to synthetic monitoring storage resources."
}
