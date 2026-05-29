locals {
  prefix    = "selc"
  env_short = "p"
  env       = "prod"

  project = "${local.prefix}-${local.env_short}"
}