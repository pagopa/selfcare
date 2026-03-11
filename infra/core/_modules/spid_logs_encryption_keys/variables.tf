
variable "tags" {
  type    = map(any)
  default = {}
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for storing connection strings"
}
