locals {
  prefix    = "selc"
  env_short = "u"
  env       = "uat"

  project = "${local.prefix}-${local.env_short}"
}