variable "environment" {
  type = object({
    prefix          = string
    env_short       = string
    location        = string
    location_short  = string
    domain          = optional(string)
    app_name        = string
    instance_number = string
  })
  description = "Naming inputs for the Storage Account hosting the webhook queue."
}

variable "resource_group_name" {
  type        = string
  description = "Resource group where the storage account and Container Apps Environment identity reside."
}

variable "private_endpoint_subnet_name" {
  type        = string
  description = "Existing subnet reserved for private endpoints."
}

variable "virtual_network_name" {
  type        = string
  description = "Virtual network containing the private endpoint subnet."
}

variable "virtual_network_resource_group_name" {
  type        = string
  description = "Resource group containing the virtual network."
}

variable "private_dns_zone_resource_group_name" {
  type        = string
  description = "Resource group containing the Storage Queue private DNS zone."
}

variable "container_app_environment_identity_name" {
  type        = string
  description = "User-assigned managed identity shared by the Container Apps Environment."
}

variable "log_analytics_workspace_name" {
  type        = string
  description = "Log Analytics Workspace that receives storage account diagnostics."
}

variable "log_analytics_workspace_resource_group_name" {
  type        = string
  description = "Resource group containing the Log Analytics Workspace."
}

variable "subscription_id" {
  type        = string
  description = "Subscription containing the Storage Queue."
}

variable "queue_name" {
  type        = string
  description = "Name of the queue used for webhook delivery notifications."
}

variable "tags" {
  type        = map(string)
  description = "Tags applied to the Storage Account."
}
