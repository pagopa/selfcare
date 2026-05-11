# general
env            = "prod"
env_short      = "p"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  name                = "selc-p-kv"
  resource_group_name = "selc-p-sec-rg"
  pat_secret_name     = "github-runner-pat"
}

law = {
  name                = "selc-p-law"
  resource_group_name = "selc-p-monitor-rg"
}
