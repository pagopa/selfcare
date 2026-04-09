###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-uat-pnpg"
}
locals {
  # ca_name = "selc-${module.local.config.env_short}-party-reg-proxy-ca"

  registry_proxy_container_app = {
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

  registry_proxy_app_settings = [
    {
      name = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -XX:MaxRAMPercentage=75.0"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "MS_PARTY_REGISTRY_PROXY_LOG_LEVEL"
      value = "DEBUG"
    },
    {
      name  = "MOCK_OPEN_DATA_ENABLED"
      value = "false"
    },
    {
      name  = "MOCK_OPEN_DATA_URL"
      value = "https://selc${module.local.config.env_short}weupnpgcheckoutsa.z6.web.core.windows.net/resources"
    },
    {
      name  = "MOCK_OPEN_DATA_INSTITUTION_ENDPOINT"
      value = "/institutions-open-data-mock.csv"
    },
    {
      name  = "MOCK_OPEN_DATA_CATEGORY_ENDPOINT"
      value = "/categories-open-data-mock.csv"
    },
    {
      name  = "MOCK_OPEN_DATA_AOO_ENDPOINT"
      value = "/aoo-open-data-mock.csv"
    },
    {
      name  = "MOCK_OPEN_DATA_UO_ENDPOINT"
      value = "/uo-open-data-mock.csv"
    },
    {
      name  = "INFO_CAMERE_URL"
      value = "https://icapiscl.infocamere.it"
    },
    {
      name  = "INFO_CAMERE_INSTITUTIONS_BY_LEGAL_ENDPOINT"
      value = "/ic/ce/wspa/wspa/rest/listaLegaleRappresentante/{taxId}"
    },
    {
      name  = "INFO_CAMERE_AUTHENTICATION_ENDPOINT"
      value = "/ic/ce/wspa/wspa/rest/authentication"
    },
    {
      name  = "NATIONAL_REGISTRIES_URL"
      value = "https://api-selcpg.${module.local.config.env}.notifichedigitali.it/national-registries-private"
    },
    {
      name  = "PDND_BASE_URL"
      value = "https://auth.interop.pagopa.it"
    },
    {
      name  = "PDND_INFOCAMERE_AUDIENCE"
      value = "auth.interop.pagopa.it/client-assertion"
    },
    {
      name  = "IVASS_BASE_URL"
      value = "https://infostat-ivass.bancaditalia.it"
    },
    {
      name  = "REDIS_URL"
      value = "selc-${module.local.config.env_short}-weu-pnpg-redis.redis.cache.windows.net"
    },
    {
      name  = "REDIS_PORT"
      value = "6380"
    }
  ]

  secrets_names = {
    "BLOB_STORAGE_CONN_STRING"              = "web-storage-connection-string"
    "NATIONAL_REGISTRY_API_KEY"             = "national-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                  = "jwt-public-key"
    "PDND_INFOCAMERE_PRIVATE_KEY"           = "infocamere-interop-certificate-client-private-key"
    "PDND_INFOCAMERE_CLIENT_ID"             = "infocamere-interop-client-id"
    "PDND_INFOCAMERE_KID"                   = "infocamere-interop-kid"
    "PDND_INFOCAMERE_PURPOSE_ID"            = "infocamere-interop-purpose-id"
    "REDIS_PASSWORD"                        = "redis-primary-access-key"
  }

  app_settings = local.registry_proxy_app_settings

  probes = [
    {
      httpGet = {
        path   = "actuator/health"
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
        path   = "actuator/health"
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
        path   = "actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      failureThreshold    = 30
      type                = "Startup"
      initialDelaySeconds = 60
    }
  ]
}


###############################################################################
# Container App
###############################################################################

module "container_app_registry_proxy_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = local.registry_proxy_container_app
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-party-reg-proxy"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-ms-party-registry-proxy"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = local.probes
  tags                           = module.local.config.tags
}
