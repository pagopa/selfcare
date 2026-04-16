variable "domain" {
  type        = string
  description = "Domain"
}

variable "search_service_id" {
  type        = string
  description = "Resource ID of the AI Search service"
}

variable "srch_service_name" {
  type        = string
  description = "AI Search service name"
}

variable "srch_service_primary_key" {
  type        = string
  description = "AI Search service primary key"
}

variable "api_version" {
  type        = string
  description = "API version to use for Azure Search REST API calls"
  default     = "2024-07-01"
}