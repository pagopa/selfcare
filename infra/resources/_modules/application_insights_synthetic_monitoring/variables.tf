variable "prefix" {
  type        = string
  description = "Prefix used for synthetic monitoring resources."
}

variable "location" {
  type        = string
  description = "Azure region where resources are created."
}

variable "resource_group_name" {
  type        = string
  description = "Resource group used for monitoring resources."
}

variable "container_app_environment_id" {
  type        = string
  description = "Container Apps Environment used to run the private synthetic probe."
}

variable "user_assigned_identity_id" {
  type        = string
  description = "User-assigned managed identity attached to the monitoring job."
}

variable "user_assigned_identity_client_id" {
  type        = string
  description = "Client ID selected by DefaultAzureCredential."
}

variable "user_assigned_identity_principal_id" {
  type        = string
  description = "Principal ID granted read access to the monitoring table."
}

variable "storage_account_name" {
  type        = string
  description = "Shared synthetic monitoring Storage Account created by the core stack."
}

variable "storage_account_resource_group_name" {
  type        = string
  description = "Resource group containing the shared synthetic monitoring Storage Account."
}

variable "image_tag" {
  type        = string
  description = "Webhook image tag containing the managed-identity synthetic probe."
}

variable "application_insight_name" {
  type        = string
  description = "Application Insights component receiving synthetic health metrics."
}

variable "application_insight_rg_name" {
  type        = string
  description = "Resource group containing Application Insights."
}

variable "application_insights_action_group_ids" {
  type        = list(string)
  description = "Action groups notified by the synthetic health alert."
}

variable "diagnostics_url" {
  type        = string
  description = "Private diagnostics endpoint queried by the synthetic probe."
}

variable "cron_scheduling" {
  type        = string
  description = "Cron schedule for the synthetic monitoring job."
  default     = "*/5 * * * *"
}

variable "tags" {
  type        = map(any)
  description = "Tags applied to monitoring resources."
}
