variable "env_short" {
  type = string
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID"
}

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Application gateway: api certificate name on Key Vault"
}

variable "app_gateway_api_pnpg_certificate_name" {
  type        = string
  description = "Application gateway: api-pnpg certificate name on Key Vault"
}
