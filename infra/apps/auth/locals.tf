locals {
  prefix            = "selc"
  domain            = "pnpg"
  location_short    = "weu"
  pnpg_suffix       = var.is_pnpg == true ? "-pnpg" : ""

  container_app_environment_name = "${local.prefix}-${var.env_short}${local.pnpg_suffix}-${var.cae_name}"
  ca_resource_group_name         = "${local.prefix}-${var.env_short}-container-app${var.suffix_increment}-rg"
}