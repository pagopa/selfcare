prefix           = "selc"
env_short        = "p"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_config = {
  cpu    = 2
  memory = 1
}

container_app = {
  min_replicas = 1
  max_replicas = 5
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "1"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 19 * * MON-FRI"
          "timezone"        = "Europe/Rome"
        }
        type = "cron"
      }
      name = "cron-scale-rule"
    }
  ]
  cpu    = 1
  memory = "2Gi"
}

enable_sws               = true
enable_ca_sws            = true
cidr_subnet_namirial_sws = ["10.1.150.0/29"]
app_settings             = []
environment_variables    = {}
