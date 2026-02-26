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
