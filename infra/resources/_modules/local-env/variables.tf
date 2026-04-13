# ==============================================================================
# Environment identity — required, no defaults
# ==============================================================================

variable "env" {
  type        = string
  description = "Environment name: dev, uat, prod"
}

variable "env_short" {
  type        = string
  description = "Environment short code: d, u, p"
}

variable "domain" {
  type        = string
  description = "Domain: ar or pnpg"
}

# ==============================================================================
# DNS
# ==============================================================================

variable "dns_zone_prefix" {
  type        = string
  description = "DNS zone prefix (e.g. dev.selfcare, selfcare, pnpg.dev.selfcare)"
}

variable "api_dns_zone_prefix" {
  type        = string
  description = "API DNS zone prefix (e.g. api.dev.selfcare, api-pnpg.selfcare)"
}

variable "external_domain" {
  type        = string
  description = "External domain suffix (pagopa.it for ar/dev-pnpg, it for uat/prod-pnpg)"
  default     = "pagopa.it"
}

# ==============================================================================
# Container App Environment — required, not derivable cleanly
# ==============================================================================

variable "private_dns_name_domain" {
  type        = string
  description = "CAE private DNS domain suffix (e.g. whitemoss-eb7ef327.westeurope.azurecontainerapps.io)"
}

variable "container_app_environment_name" {
  type        = string
  description = "Name of the Azure Container App Environment (e.g. selc-d-cae-002)"
}

variable "ca_resource_group_name" {
  type        = string
  description = "Resource group name of the Container App Environment"
}

# ==============================================================================
# Container App sizing — optional with dev/non-prod defaults
# ==============================================================================

variable "container_app_min_replicas" {
  type        = number
  description = "Minimum number of container app replicas"
  default     = 1
}

variable "container_app_max_replicas" {
  type        = number
  description = "Maximum number of container app replicas"
  default     = 1
}

variable "container_app_desired_replicas" {
  type        = string
  description = "Desired replicas for the cron scale rule"
  default     = "1"
}

variable "container_app_cpu" {
  type        = number
  description = "CPU cores allocated to each container app replica"
  default     = 0.5
}

variable "container_app_memory" {
  type        = string
  description = "Memory allocated to each container app replica (e.g. 1Gi, 2.5Gi)"
  default     = "1Gi"
}
