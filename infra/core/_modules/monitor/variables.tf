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

variable "tags" {
  type    = map(any)
  default = {}
}

variable "subscription_id" {
  type        = string
  description = "Azure subscription ID"
}

# From key_vault module
variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for reading secrets and writing app insights key"
}

# From log_analytics module
variable "monitor_rg_name" {
  type        = string
  description = "Monitor resource group name (from log_analytics module)"
}

variable "monitor_rg_location" {
  type        = string
  description = "Monitor resource group location (from log_analytics module)"
}

variable "application_insights_id" {
  type        = string
  description = "Application Insights ID (from log_analytics module)"
}

variable "application_insights_name" {
  type        = string
  description = "Application Insights name (from log_analytics module)"
}

# Web test URLs - from dns_public and cdn modules
variable "dns_a_api_fqdn" {
  type        = string
  description = "FQDN of the api DNS A record"
}

variable "dns_a_api_pnpg_fqdn" {
  type        = string
  description = "FQDN of the api-pnpg DNS A record"
}

variable "cdn_fqdn" {
  type        = string
  description = "CDN FQDN for web test"
}

# Selfcare status action group emails (from secrets)
variable "selfcare_status_dev_email" {
  type        = string
  description = "Dev status alert email"
  default     = ""
}

variable "selfcare_status_dev_slack" {
  type        = string
  description = "Dev status alert slack email"
  default     = ""
}

variable "selfcare_status_uat_email" {
  type        = string
  description = "UAT status alert email"
  default     = ""
}

variable "selfcare_status_uat_slack" {
  type        = string
  description = "UAT status alert slack email"
  default     = ""
}
