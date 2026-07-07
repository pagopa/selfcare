locals {
  environment = {
    prefix          = var.prefix
    env_short       = var.env_short
    location        = var.location
    domain          = var.domain
    app_name        = var.app_name
    instance_number = var.instance_number
  }

  project = "selc-${var.env_short}"

  naming_config            = "user-attachments"
  resource_group_name_vnet = "${local.project}-vnet-rg"
}

