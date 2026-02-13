variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "env" {
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

# From key_vault module (needed if role assignments are uncommented)
variable "adgroup_developers_object_id" {
  type    = string
  default = ""
}

variable "adgroup_admin_object_id" {
  type    = string
  default = ""
}
