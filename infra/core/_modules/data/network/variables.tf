locals {
  product = "${var.prefix}-${var.env_short}"

  vnet_core_name                = "${local.product}-vnet"
  vnet_core_resource_group_name = "${local.product}-vnet-rg"
  private_endpoint_subnet_name  = "${local.product}-private-endpoints-snet"
}

variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "redis_private_endpoint_enabled" {
  type    = bool
  default = true
}