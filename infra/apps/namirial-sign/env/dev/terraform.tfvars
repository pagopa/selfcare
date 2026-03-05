prefix           = "selc"
env_short        = "d"
suffix_increment = "-002"
cae_name         = "cae-002"
location         = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_config = {
  cpu    = 0.5
  memory = 1
}

container_app = {
  min_replicas = 1
  max_replicas = 1
  scale_rules  = []
  cpu          = 0.5
  memory       = "1Gi"
}

environment_variables = {
  SPRINGDOC_API_DOCS_ENABLED = true
}

app_settings = [
  {
    name  = "SPRINGDOC_API_DOCS_ENABLED",
    value = true
  }
]

enable_sws    = true
enable_ca_sws = true
