env_short        = "d"
suffix_increment = "-002"
cae_name         = "cae-002"
prefix           = "selc"
env              = "dev"
location         = "westeurope"
is_pnpg          = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-d-pnpg-sec-rg"
  name                = "selc-d-pnpg-kv"
}

key_vault_pnpg = {
  resource_group_name = "selc-d-pnpg-sec-rg"
  name                = "selc-d-pnpg-kv"
}
