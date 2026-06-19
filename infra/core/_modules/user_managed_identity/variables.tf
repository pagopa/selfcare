
variable "location" {
  type        = string
  description = "Azure region"
}

variable "prefix" {
  type        = string
  description = "Prefix for resource names"
  default     = "selc"
}

variable "env_short" {
  type        = string
  description = "Short environment name"
  validation {
    condition     = length(var.env_short) == 1
    error_message = "Short environment name must be exactly 1 character."
  }
}

variable "domain" {
  type        = string
  description = "Domain name for resource naming"
}

variable "tags" {
  type        = map(any)
  description = "Resource tags"
}

variable "product_storage_name" {
  type        = string
  description = "Complete name for the product storage"
}

variable "product_storage_rg" {
  type        = string
  description = "Resource group name for the product storage"
}

variable "documents_storage_name" {
  type        = string
  description = "Complete name for the documents storage"
}

variable "documents_storage_rg" {
  type        = string
  description = "Resource group name for the documents storage"
}
