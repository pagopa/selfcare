# general
env            = "uat"
env_short      = "u"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  name                = "selc-u-kv"
  resource_group_name = "selc-u-sec-rg"
  pat_secret_name     = "github-runner-pat"
}

law = {
  name                = "selc-u-law"
  resource_group_name = "selc-u-monitor-rg"
}
