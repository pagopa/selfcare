locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  project     = "selc-${var.env_short}"
}