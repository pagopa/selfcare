variable "storage_account_name" {
  description = "Name of the storage account for Terraform state"
  type        = string
}

variable "storage_resource_group_name" {
  description = "Resource group of the storage account for Terraform state"
  type        = string
}

variable "storage_container_name" {
  description = "Container name in the storage account for Terraform state"
  type        = string
}

variable "prefix" {
  description = "Project prefix"
  type        = string
}

variable "env_short" {
  description = "Short environment identifier"
  type        = string
}

variable "storage_role_name" {
  description = "Role definition name to assign on the storage account"
  type        = string
}
