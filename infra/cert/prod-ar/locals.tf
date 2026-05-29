locals {
  env_short      = "p"
  domain         = ""
  location_short = ""

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Prod"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare/cert/jwt"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }
}
