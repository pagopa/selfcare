variable "name" {
  description = "Name of the proxy Container App."
  type        = string
}

variable "resource_group_name" {
  description = "Resource group containing the internal Container Apps environment."
  type        = string
}

variable "container_app_environment_name" {
  description = "Name of the internal environment shared with auth."
  type        = string
}

variable "vnet_resource_group_name" {
  description = "Resource group containing the virtual network and private DNS zone."
  type        = string
}

variable "virtual_network_name" {
  description = "Virtual network containing the Function integration subnet."
  type        = string
}

variable "function_subnet_name" {
  description = "Onboarding Function integration subnet whose IPv4 prefixes are allowed."
  type        = string
}

variable "auth_container_app_name" {
  description = "Name of the auth Container App in the same environment."
  type        = string
}

variable "image" {
  description = "Auth image containing the standalone proxy entry point."
  type        = string
}

variable "tags" {
  description = "Tags assigned to the proxy Container App."
  type        = map(string)
}
