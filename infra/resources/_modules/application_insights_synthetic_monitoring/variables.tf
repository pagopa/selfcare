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

variable "application_insight_name" {
  type        = string
  description = "Application Insights component receiving availability telemetry."
}

variable "application_insight_rg_name" {
  type        = string
  description = "Resource group containing Application Insights."
}

variable "application_insights_action_group_ids" {
  type        = list(string)
  description = "Action groups notified by the availability alerts."
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
