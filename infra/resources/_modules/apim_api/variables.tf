# General Variables
variable "apim_name" {
  type        = string
  description = "The name of the API Management instance."
}

variable "apim_rg" {
  type        = string
  description = "The name of the resource group in which the API Management instance exists."
}

variable "api_name" {
  type        = string
  description = "The name of the API in the API Management instance."
}

variable "display_name" {
  type        = string
  description = "The display name of the API in the API Management instance."
}

variable "base_path" {
  type        = string
  description = "The base path of the API in the API Management instance."
}

variable "private_dns_name" {
  type        = string
  description = "The private DNS name of the API in the API Management instance."
}

variable "dns_zone_prefix" {
  type        = string
  default     = "selfcare"
  description = "The dns subdomain."
}

variable "api_dns_zone_prefix" {
  type        = string
  default     = "api.selfcare"
  description = "The dns subdomain."
}

variable "external_domain" {
  type        = string
  default     = "pagopa.it"
  description = "Domain for delegation"
}

variable "openapi_path" {
  type        = string
  description = "Path to the OpenAPI specification file."
}