locals {
  project      = "${var.prefix}-${var.env_short}%{if var.domain != ""}-${var.domain}%{endif}"
  project_full = "${var.prefix}-${var.env_short}%{if var.location_short != ""}-${var.location_short}%{endif}%{if var.domain != ""}-${var.domain}%{endif}"
  vnet_name    = "${local.project}-vnet-rg"
}

