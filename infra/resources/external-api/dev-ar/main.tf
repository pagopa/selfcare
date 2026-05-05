###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
}

locals {
  ca_base_name = "selc-${module.local.config.env_short}-ext-api-backend"
  ca_name      = "${local.ca_base_name}-ca"
  # storage_logs    = "selc${module.local.config.env_short}stlogs"
  # storage_logs_rg = "selc-${module.local.config.env_short}-logs-storage-rg"

  container_app = {
    min_replicas = module.local.config.container_app.min_replicas
    max_replicas = module.local.config.container_app.max_replicas
    scale_rules  = module.local.config.container_app.scale_rules
    cpu          = 1.0
    memory       = "2Gi"
  }

  spring_boot_health_probes = [
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Liveness"
      failureThreshold    = 3
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Readiness"
      failureThreshold    = 30
      initialDelaySeconds = 30
    },
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Startup"
      failureThreshold    = 30
      initialDelaySeconds = 60
    }
  ]
}