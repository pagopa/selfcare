
variable "name" {
  type        = string
  description = "The name of the spid testenv eg. selc-d-wew-pnpg"
}

variable "enable_spid_test" {
  type        = bool
  description = "to provision italia/spid-testenv2:1.1.0"
  default     = false
}

variable "location" {
  type = string
}

variable "tags" {
  type = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

variable "spid_testenv_local_config_dir" {
  type = string
}

variable "hub_spid_login_metadata_url" {
  type        = string
  description = "The url of the spid login metadata endpoint of the hub environment, used by spid-testenv to fetch the identity providers configuration"
}


variable "key_vault_id" {
  type        = string
  description = "Key Vault ID"
}