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
  description = "Naming inputs for the Service Bus namespace."
}

variable "resource_group_name" {
  type        = string
  description = "Resource group where the namespace and Container Apps Environment identity reside."
}

variable "sku" {
  type        = string
  description = "Service Bus SKU. Standard uses a firewall-restricted public endpoint; Premium creates a private endpoint."

  validation {
    condition     = contains(["Standard", "Premium"], var.sku)
    error_message = "The Service Bus SKU must be either Standard or Premium."
  }
}

variable "outbound_public_ip_name" {
  type        = string
  description = "Static public IP used as the outbound address of the Container Apps Environment."
  default     = null

  validation {
    condition     = var.sku != "Standard" || var.outbound_public_ip_name != null
    error_message = "outbound_public_ip_name is required when sku is Standard."
  }
}

variable "outbound_public_ip_resource_group_name" {
  type        = string
  description = "Resource group containing the outbound public IP."
  default     = null

  validation {
    condition     = var.sku != "Standard" || var.outbound_public_ip_resource_group_name != null
    error_message = "outbound_public_ip_resource_group_name is required when sku is Standard."
  }
}

variable "private_endpoint_subnet_name" {
  type        = string
  description = "Existing subnet reserved for private endpoints."
  default     = null

  validation {
    condition     = var.sku != "Premium" || var.private_endpoint_subnet_name != null
    error_message = "private_endpoint_subnet_name is required when sku is Premium."
  }
}

variable "virtual_network_name" {
  type        = string
  description = "Virtual network containing the private endpoint subnet."
  default     = null

  validation {
    condition     = var.sku != "Premium" || var.virtual_network_name != null
    error_message = "virtual_network_name is required when sku is Premium."
  }
}

variable "virtual_network_resource_group_name" {
  type        = string
  description = "Resource group containing the virtual network."
  default     = null

  validation {
    condition     = var.sku != "Premium" || var.virtual_network_resource_group_name != null
    error_message = "virtual_network_resource_group_name is required when sku is Premium."
  }
}

variable "private_dns_zone_resource_group_name" {
  type        = string
  description = "Resource group containing the Service Bus private DNS zone."
  default     = null

  validation {
    condition     = var.sku != "Premium" || var.private_dns_zone_resource_group_name != null
    error_message = "private_dns_zone_resource_group_name is required when sku is Premium."
  }
}

variable "container_app_environment_identity_name" {
  type        = string
  description = "User-assigned managed identity shared by the Container Apps Environment."
}

variable "log_analytics_workspace_name" {
  type        = string
  description = "Log Analytics Workspace that receives Service Bus diagnostics."
}

variable "log_analytics_workspace_resource_group_name" {
  type        = string
  description = "Resource group containing the Log Analytics Workspace."
}

variable "subscription_id" {
  type        = string
  description = "Subscription containing the Service Bus queue."
}

variable "queue_name" {
  type        = string
  description = "Name of the queue used for webhook delivery notifications."
}

variable "tags" {
  type        = map(string)
  description = "Tags applied to the Service Bus namespace."
}
