locals {
  prefix         = "selc"
  env_short      = "u"
  location       = "westeurope"
  location_short = "weu"
  domain         = "ar"
  project        = "${local.prefix}-${local.env_short}-${local.location_short}-${local.domain}"

  tags = {
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
    CreatedBy   = "Terraform"
    Environment = "Uat"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-ms-party-registry-proxy/infra/search_engine"
  }
}
