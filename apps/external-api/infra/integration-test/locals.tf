locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  project     = "selc-${var.env_short}"
  env_url     = var.env_short == "p" ? "" : ".${var.env}"
  env         = var.env

  container_app_environment_name = "${local.project}${local.pnpg_suffix}-${var.cae_name}"
  ca_resource_group_name         = "${local.project}-container-app${var.suffix_increment}-rg"
  monitor_rg_name                = "${local.project}-monitor-rg"
  monitor_appinsights_name       = "${local.project}-appinsights"
}
