locals {
  project     = "selc-${var.env_short}"

  container_app_environment_name = "${local.project}-${var.cae_name}"
  ca_resource_group_name         = "${local.project}-container-app${var.suffix_increment}-rg"
}