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

variable "dns_default_ttl_sec" {
  type    = number
  default = 3600
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

variable "dns_zone_prefix_ar" {
  type        = string
  default     = "areariservat"
  description = "The dns subdomain for areariservata."
}

variable "dns_ns_interop_selfcare" {
  type        = list(string)
  description = "NS records for interop delegation"
  default     = null
}

# From network module
variable "rg_vnet_name" {
  type = string
}

variable "appgateway_public_ip_address" {
  type        = string
  description = "Public IP address of the Application Gateway"
}
