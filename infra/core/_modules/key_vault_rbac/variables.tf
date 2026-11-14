variable "prefix" {
  type        = string
  description = "Resource naming prefix"
}

variable "env_short" {
  type        = string
  description = "Short environment name: d, u or p"
}

variable "location" {
  type        = string
  description = "Azure region"
}

variable "domain" {
  type        = string
  description = "Project domain used in resource names, for example ar"
}

variable "resource_group_name" {
  type        = string
  description = "Core security resource group containing the project vault"
}

variable "tenant_id" {
  type        = string
  description = "Microsoft Entra tenant ID"
}

variable "networking" {
  type = object({
    resource_group_name        = string
    virtual_network_id         = string
    private_endpoint_subnet_id = string
  })
  description = "Existing core network for the vault private endpoint and DNS"
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
}
