variable "is_pnpg" {
  type        = bool
  default     = false
  description = "(Optional) True if you want to apply changes to PNPG environment"
}

variable "prefix" {
  description = "Domain prefix"
  type        = string
  default     = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env_short" {
  description = "Environment short name"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "tags" {
  type = map(any)
}


variable "subscription" {
  type = string
}


variable "location" {
  type    = string
  default = "westeurope"
}
