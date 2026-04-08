###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-prod-ar"
}

# ###############################################################################
# # Namirial Sign Web Service
# ###############################################################################
module "namirial_sign" {
  source = "../../_modules/namirial_sws"

  prefix                           = module.local.config.env_short
  env_short                        = module.local.config.env_short
  cae_name                         = module.local.config.container_app_environment_name
  tags                             = module.local.config.tags
  cidr_subnet_namirial_sws         = ["10.1.150.0/29"]
  cross_tenant_replication_enabled = true
  enable_sws                       = true
  enable_ca_sws                    = true
  suffix_increment                 = "-002"

  environment_variables = {
    SPRINGDOC_API_DOCS_ENABLED = true
  }
  container_config = {
    cpu    = 0.5
    memory = 1
  }
  container_app = {
    min_replicas = 1
    max_replicas = 5
    scale_rules  = []
    cpu          = 1
    memory       = "2Gi",
    scale_rules = [
      {
        name = "cron-scale-rule"
        custom = {
          metadata = {
            "desiredReplicas" = "3"
            "start"           = "0 8 * * MON-FRI"
            "end"             = "0 19 * * MON-FRI"
            "timezone"        = "Europe/Rome"
          }
          type = "cron"
        }
      }
  ] }
  app_settings = [
    {
      name  = "SPRINGDOC_API_DOCS_ENABLED",
      value = true
    }
  ]
}
