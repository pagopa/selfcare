variable "project" {
  type        = string
  description = "The name of the project for eg. selc-d or selc-d-pnpg"
}

variable "prefix" {
  type        = string
  description = "The prefix for the project, e.g., selc"
  default     = "selc"
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

variable "azdo_sp_tls_cert_enabled" {
  type    = string
  default = false
}

variable "azuread_service_principal_azure_cdn_frontdoor_id" {
  type    = string
  default = "f3b3f72f-4770-47a5-8c1e-aa298003be12"
}

variable "sku_name" {
  type    = string
  default = "standard"
}

variable "soft_delete_retention_days" {
  type        = number
  default     = 15
  description = "(Optional) The number of days that items should be retained for once soft-deleted. This value can be between 7 and 90 (the default) days."
}

variable "adgroup_admin_object_id" {
  type        = string
  description = "Object ID of the Azure AD admin group (e.g. selc-d-adgroup-admin)"
}

variable "adgroup_developers_object_id" {
  type        = string
  description = "Object ID of the Azure AD developers group (e.g. selc-d-adgroup-developers)"
}

variable "adgroup_externals_object_id" {
  type        = string
  description = "Object ID of the Azure AD externals group (e.g. selc-d-adgroup-externals)"
  default     = null
}

variable "adgroup_security_object_id" {
  type        = string
  description = "Object ID of the Azure AD security group (e.g. selc-d-adgroup-security)"
  default     = null
}