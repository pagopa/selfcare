variable "project" {
  type = string
}

variable "env_short" {
  type        = string
  description = "Short environment name (e.g., d, p)"
}

variable "project_pair" {
  type = string
}

variable "location" {
  type = string
}

variable "location_pair" {
  type = string
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "vnet_name" {
  type        = string
  description = "VNet name for the VNet (for DNS zone)"
}

variable "rg_vnet_name" {
  type        = string
  description = "Resource group name for the VNet (for DNS zone)"
}

variable "rg_pair_vnet_name" {
  type        = string
  description = "Resource group name for the paired VNet"
}

variable "subscription_id" {
  type        = string
  description = "Subscription ID for the paired VNet"
}

variable "subscription_name" {
  type        = string
  description = "Subscription name for the paired VNet"
}

variable "vnet_pair_name" {
  type        = string
  description = "VNet name for the paired VNet"
}
# module.vnet_pariname
# var.sudata.azurerm_subscription.current.display_name
#   subscription_id     = data.azurerm_subscription.current.subscription_id

variable "vpn_sku" {
  type        = string
  default     = "VpnGw1"
  description = "SKU for the VPN gateway"
}

variable "vpn_pip_sku" {
  type        = string
  default     = "Standard"
  description = "SKU for the VPN Public IP"
}

variable "vpn_snet_id" {
  type        = string
  description = "ID of the VPN subnet"
}

variable "cidr_subnet_vpn" {
  type        = list(string)
  description = "CIDR block for the VPN subnet"
}

variable "cidr_subnet_dns_forwarder" {
  type        = list(string)
  description = "CIDR block for the DNS forwarder subnet"
}

variable "cidr_subnet_pair_dnsforwarder" {
  type        = list(string)
  description = "CIDR block for the paired DNS forwarder subnet"
}

variable "tenant_id" {
  type        = string
  description = "Tenant ID for the VPN gateway"
}

variable "private_endpoint_network_policies" {
  type        = string
  description = "Private endpoint network policies"
  default     = "Enabled"
}

variable "sec_workspace_id" {
  type        = string
  description = "Log Analytics Workspace ID for security logs"
}
variable "sec_storage_id" {
  type        = string
  description = "Storage account ID for security logs"
}

