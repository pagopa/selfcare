# general
locals {
  project = "${var.prefix}-${var.env_short}"
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

variable "env" {
  type        = string
  description = "env directory name"
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

# apim
variable "apim_publisher_name" {
  type = string
}

variable "apim_sku" {
  type = string
}

variable "private_dns_name" {
  type        = string
  description = "AKS private DNS record"
}

variable "private_onboarding_dns_name" {
  type        = string
  description = "AKS private onboarding DNS record"
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

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Application gateway: api certificate name on Key Vault"
}

variable "ca_suffix_dns_private_name" {
  type        = string
  description = "CA suffix private DNS record"
}

variable "ca_pnpg_suffix_dns_private_name" {
  type        = string
  description = "CA PNPG suffix private DNS record"
}

variable "location_short" {
  type    = string
  default = "weu"
}

variable "domain" {
  type = string
  validation {
    condition = (
      length(var.domain) <= 12
    )
    error_message = "Max length is 12 chars."
  }
  default = "pnpg"
}