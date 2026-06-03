module "cert" {
  source = "../_modules/cert"

  env_short      = local.env_short
  domain         = local.domain
  location_short = local.location_short
  tags           = local.tags
}
