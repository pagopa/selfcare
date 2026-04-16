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

variable "domain" {
  type    = string
  default = ""
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "location_short" {
  type    = string
  default = ""
}

variable "cert_allowed_uses" {
  type    = list(string)
  default = ["crl_signing", "data_encipherment", "digital_signature", "key_agreement", "cert_signing", "key_encipherment"]
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

variable "tags" {
  type = map(any)
}
