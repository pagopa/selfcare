variable "prefix" {
  type        = string
  description = "Project prefix (e.g. selc)"
}

variable "env_short" {
  type        = string
  description = "Short environment name (e.g. d, u, p)"
}

variable "monitor_rg_name" {
  type        = string
  description = "Name of the monitor resource group (from log_analytics module output)"
}

variable "monitor_rg_location" {
  type        = string
  description = "Location of the monitor resource group"
}

variable "tags" {
  type        = map(any)
  description = "Azure resource tags"
}
