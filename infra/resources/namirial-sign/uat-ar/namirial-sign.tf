###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "uat"
  env_short = "u"
  domain    = "ar"

  dns_zone_prefix                = "uat.selfcare"
  api_dns_zone_prefix            = "api.uat.selfcare"
  private_dns_name_domain        = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-cae-002"
  ca_resource_group_name         = "selc-u-container-app-002-rg"
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
