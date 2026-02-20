variable "env" {
  type        = string
  description = "Environment name (e.g. dev, uat, prod)"
}

# CDN
variable "checkout_cdn_name" {
  type        = string
  description = "CDN endpoint name"
}

variable "checkout_cdn_storage_primary_access_key" {
  type        = string
  sensitive   = true
  description = "CDN storage account primary access key"
}

# Resource Group
variable "checkout_fe_rg_name" {
  type        = string
  description = "Checkout frontend resource group name"
}

variable "checkout_endpoint_name" {
  type        = string
  description = "Checkout frontend endpoint name"
}
