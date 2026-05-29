locals {
  prefix    = "selc"
  env_short = "d"
  env       = "dev"

  project = "${local.prefix}-${local.env_short}"
}