locals {
  project                                 = "${var.prefix}-${var.env_short}"
  vnet_name                               = "${local.project}-vnet-rg"
}