variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "private_endpoint_network_policies" {
  type    = string
  default = "Enabled"
}

variable "dns_zone_prefix" {
  type    = string
  default = "selfcare"
}

variable "external_domain" {
  type    = string
  default = "pagopa.it"
}

variable "aks_platform_env" {
  type = string
}

variable "auth_ms_private_dns_suffix" {
  type = string
}

variable "ca_pnpg_suffix_dns_private_name" {
  type = string
}

variable "spid_pnpg_path_prefix" {
  type    = string
  default = "/spid/v1"
}

variable "app_gateway_api_certificate_name" {
  type = string
}

variable "app_gateway_api_pnpg_certificate_name" {
  type = string
}

variable "log_analytics_workspace_id" {
  type        = string
  description = "Log Analytics workspace receiving Application Gateway diagnostics."
  default     = null
}

variable "app_gateway_access_log_enabled" {
  type        = bool
  description = "Creates a diagnostic setting sending ApplicationGatewayAccessLog to log_analytics_workspace_id. Keep false where another setting already sends that category to the same workspace (Azure rejects duplicates)."
  default     = false

  validation {
    condition     = !var.app_gateway_access_log_enabled || var.log_analytics_workspace_id != null
    error_message = "log_analytics_workspace_id is required when app_gateway_access_log_enabled is true."
  }
}

variable "app_gateway_sku_name" {
  type    = string
  default = "Standard_v2"
}

variable "app_gateway_sku_tier" {
  type    = string
  default = "Standard_v2"
}

variable "app_gateway_waf_enabled" {
  type    = bool
  default = false
}

variable "app_gateway_alerts_enabled" {
  type    = bool
  default = false
}

variable "app_gateway_min_capacity" {
  type    = number
  default = 0
}

variable "app_gateway_max_capacity" {
  type    = number
  default = 2
}

# From network module
variable "rg_vnet_name" {
  type = string
}

variable "rg_vnet_location" {
  type = string
}

variable "vnet_name" {
  type = string
}

variable "appgateway_public_ip_id" {
  type = string
}

variable "cidr_subnet_appgateway" {
  type = list(string)
}

# From key_vault module
variable "key_vault_id" {
  type = string
}

# From key_vault module
# From monitor module
variable "action_group_error_id" {
  type    = string
  default = null
}

variable "action_group_slack_id" {
  type = string
}

variable "action_group_email_id" {
  type = string
}

variable "sec_rg_name" {
  type        = string
  description = "KV resource group name, used to store appgateway secrets for sec environment"
}

variable "sec_rg_location" {
  type        = string
  description = "KV resource group location, used to store appgateway secrets for sec environment"
}

variable "tenant_id" {
  type        = string
  description = "Tenant Id"
}
