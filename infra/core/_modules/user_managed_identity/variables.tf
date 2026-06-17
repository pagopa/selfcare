
variable "location" {
  type        = string
  description = "Azure region"
}

variable "resource_group_name" {
  type        = string
  description = "Name of the resource group where resources will be created"
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

variable "is_pnpg" {
  type        = bool
  description = "Indicates if the environment is PNPG"
  default     = false
}

variable "tags" {
  type        = map(any)
  description = "Resource tags"
}
