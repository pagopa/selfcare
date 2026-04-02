###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-uat-ar"
}

# ###############################################################################
# # Namirial Sign Web Service
# ###############################################################################
module "namirial_sign" {
  source = "../../_modules/namirial_sws"

  prefix           = module.local.config.env_short
  env_short        = module.local.config.env_short
  cae_name         = module.local.config.container_app_environment_name
  tags             = module.local.config.tags
  enable_sws       = true
  enable_ca_sws    = true
  suffix_increment = "-002"
  environment_variables = {
    SPRINGDOC_API_DOCS_ENABLED = true
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
  app_settings = [
    {
      name  = "SPRINGDOC_API_DOCS_ENABLED",
      value = true
    }
  ]
}
