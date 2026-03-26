variable "prefix" {
  description = "Project prefix"
  type        = string
}

variable "env_short" {
  description = "Short environment identifier"
  type        = string
}

variable "domain" {
  description = "Domain name"
  type        = string
}

variable "app" {
  description = "Application name (used in custom role names)"
  type        = string
}

variable "env" {
  description = "Environment name (used in custom role names)"
  type        = string
}

variable "tags" {
  description = "Resource tags"
  type        = map(string)
}

variable "key_vault_id" {
  description = "Azure Key Vault ID"
  type        = string
}

variable "key_vault_pnpg_id" {
  description = "Azure Key Vault PNPG ID"
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

variable "ci_github_federations" {
  description = "GitHub federations for CI main identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "ci_github_federations_ms" {
  description = "GitHub federations for CI microservices identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "ci_github_federations_fe" {
  description = "GitHub federations for CI frontend identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "environment_ci_roles" {
  description = "RBAC roles for CI main identity"
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
}

variable "environment_ci_roles_ms" {
  description = "RBAC roles for CI microservices/frontend identity"
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
}
