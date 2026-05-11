# general
env_short      = "d"
env            = "dev"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  name                = "selc-d-kv"
  resource_group_name = "selc-d-sec-rg"
  pat_secret_name     = "github-runner-pat"
}

law = {
  name                = "selc-d-law"
  resource_group_name = "selc-d-monitor-rg"
}