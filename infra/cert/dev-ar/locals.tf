locals {
  env_short      = "d"
  domain         = ""
  location_short = ""

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare/cert/jwt"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }
}
