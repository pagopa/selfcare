locals {
  selc_project = "selc-${var.env_short}"
  repo_name    = "selfcare-external-api-backend"
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

variable "location_short" {
  type    = string
  default = "weu"
}

variable "tags" {
  type = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

variable "key_vault" {
  type = object({
    name                = string
    resource_group_name = string
    pat_secret_name     = string
  })
}

variable "law" {
  type = object({
    name                = string
    resource_group_name = string
  })
}