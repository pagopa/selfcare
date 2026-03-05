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

variable "tags" {
  type    = map(any)
  default = {}
}

variable "dns_default_ttl_sec" {
  type    = number
  default = 3600
}

variable "reverse_proxy_ip" {
  type    = string
  default = "127.0.0.1"
}

variable "redis_private_endpoint_enabled" {
  type    = bool
  default = true
}

# From network module
variable "rg_vnet_name" {
  type = string
}

variable "vnet_id" {
  type = string
}

variable "vnet_name" {
  type = string
}

variable "vnet_pair_id" {
  type = string
}

variable "vnet_pair_name" {
  type = string
}

variable "vnet_aks_platform_id" {
  type = string
}

variable "vnet_aks_platform_name" {
  type = string
}
