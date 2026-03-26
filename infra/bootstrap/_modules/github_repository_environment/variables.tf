variable "env" {
  description = "Base environment name (e.g. prod, uat, dev)"
  type        = string
}

variable "env_suffix" {
  description = "Environment suffix: ci or cd"
  type        = string
}

variable "repository" {
  description = "GitHub repository name"
  type        = string
}

variable "branch_policy_enabled" {
  description = "Whether branch policy is enabled for this environment"
  type        = bool
}

variable "repository_environment" {
  description = "GitHub repository environment configuration"
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = list(string)
    branch_pattern         = optional(string)
  })
}

variable "env_secrets" {
  description = "Map of GitHub secret name to plaintext value"
  type        = map(string)
  default     = {}
}

variable "key_vault_id" {
  description = "Azure Key Vault ID for reading secrets (required when kv_secrets is non-empty)"
  type        = string
  default     = null
}

variable "kv_secrets" {
  description = "Map of GitHub secret name to Key Vault secret name"
  type        = map(string)
  default     = {}
}
