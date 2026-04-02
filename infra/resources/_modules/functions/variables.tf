variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type = map(any)
}

variable "functions_name" {
  type        = string
  description = "Name of the onboarding function app"
}

variable "service_plan_sku" {
  type        = string
  description = "SKU of the service plan for the function app"
  default     = "B2"
}

variable "subnet_cidr" {
  type        = list(string)
  description = "Network address space."
  default     = []
}

variable "always_on" {
  type        = bool
  description = "Always on for the function app"
  default     = false
}

variable "service_plan_worker_count" {
  type        = number
  description = "Worker count"
  default     = 1
}

variable "nat_resource_group_name" {
  type        = string
  description = "Name of NAT Resource Group"
}

variable "nat_gateway_name" {
  type        = string
  description = "Name of NAT Gateway"
}

variable "replication_type" {
  type        = string
  description = "Storage account replication type"
  default     = "LRS"
}

variable "app_settings" {
  type        = map(string)
  description = "Settings references to be set as app settings in the function app"
}

variable "vnet_resource_group_name" {
  type        = string
  description = "Name of the Virtual Network Resource Group"
}

variable "vnet_name" {
  type        = string
  description = "Name of the Virtual Network"
}

variable "key_vault_id" {
  type = string
}

variable "tenant_id" {
  description = "Azure tenant ID"
  type        = string
}
