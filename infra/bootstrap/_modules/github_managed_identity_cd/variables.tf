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

variable "cd_github_federations" {
  description = "GitHub federations for CD main identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "cd_github_federations_ms" {
  description = "GitHub federations for CD microservices identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "cd_github_federations_fe" {
  description = "GitHub federations for CD frontend identity"
  type = list(object({
    repository = string
    subject    = string
  }))
}

variable "environment_cd_roles" {
  description = "RBAC roles for CD main identity"
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
}

variable "environment_cd_roles_ms" {
  description = "RBAC roles for CD microservices/frontend identity"
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
}
