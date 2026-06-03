locals {
  env_short      = "p"
  domain         = "pnpg"
  location_short = "weu"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Prod"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare/cert/jwt"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
    Application = "PNPG"
  }
}
