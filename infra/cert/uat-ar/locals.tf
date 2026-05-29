locals {
  env_short      = "u"
  domain         = ""
  location_short = ""

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Uat"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare/cert/jwt"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }
}
