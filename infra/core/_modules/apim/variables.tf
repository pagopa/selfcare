# general
locals {
  project                       = "${var.prefix}-${var.env_short}"
  apim_cert_name_proxy_endpoint = "${local.project}-proxy-endpoint-cert"
  api_domain                    = "api.${var.dns_zone_prefix}.${var.external_domain}"
  logo_api_domain               = "${var.dns_zone_prefix}.${var.external_domain}"
  apim_base_url                 = "${azurerm_api_management_custom_domain.api_custom_domain.gateway[0].host_name}/external"
}


variable "prefix" {
  type    = string
  default = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env_short" {
  type = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

variable "dns_zone_prefix" {
  type        = string
  default     = "selfcare"
  description = "The dns subdomain."
}

variable "external_domain" {
  type        = string
  default     = "pagopa.it"
  description = "Domain for delegation"
}

variable "cidr_subnet_apim" {
  type        = list(string)
  description = "Address prefixes subnet api management."
  default     = null
}

# apim
variable "apim_publisher_name" {
  type = string
}

variable "apim_sku" {
  type = string
}

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Application gateway: api certificate name on Key Vault"
}

variable "application_insight_enabled" {
  type        = bool
  description = "Logger application insight enabled"
  default     = false
}