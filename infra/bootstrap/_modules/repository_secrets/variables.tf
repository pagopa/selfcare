variable "github_federations_fe" {
  description = "Micro-frontend mapping for GitHub Workload Identity Federation"
  type        = map(string)
  default     = {}
}

variable "github_federations" {
  description = "Micro-services mapping for GitHub Workload Identity Federation"
  type        = map(string)
  default     = {}
}

variable "gh_pat_variable" {
  description = "Github pat key"
  type        = string
}

variable "tenant_id" {
  description = "Azure tenant ID"
  type        = string
}

variable "subscription_id" {
  description = "Azure subscription ID"
  type        = string
}

variable "fe_cd_identity_client_id" {
  description = "Client ID of the Azure AD application for the frontend CD pipeline"
  type        = string
}

variable "fe_ci_identity_client_id" {
  description = "Client ID of the Azure AD application for the frontend CI pipeline"
  type        = string
}

variable "cd_identity_client_id" {
  description = "Client ID of the Azure AD application for the backend CD pipeline"
  type        = string
}

variable "ci_identity_client_id" {
  description = "Client ID of the Azure AD application for the backend CI pipeline"
  type        = string
}

variable "opex_cd_identity_client_id" {
  description = "Client ID of the Azure AD application for the OPEX CD pipeline"
  type        = string
  default     = null
}

variable "opex_ci_identity_client_id" {
  description = "Client ID of the Azure AD application for the OPEX CI pipeline"
  type        = string
  default     = null
}

variable "opex" {
  description = "Opex Dashboards configuration enabled"
  type        = bool
  default     = false
}