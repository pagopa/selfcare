variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for storing application insights key"
}

variable "law_sku" {
  type    = string
  default = "PerGB2018"
}

variable "law_retention_in_days" {
  type    = number
  default = 30
}

variable "law_daily_quota_gb" {
  type    = number
  default = -1
}
