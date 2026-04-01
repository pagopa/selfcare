locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "u"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"

  is_pnpg = true

  pnpg_suffix = local.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-account"
    mongodb_name                  = "selcOnboarding"
  }

  container_app_environment_name = "${local.prefix}-${local.env_short}-${local.domain}-cae-001"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-001-rg"

  function_name = "${local.storage_prefix}-onboarding-fn"

  container_app = {
    min_replicas = 1
    max_replicas = 2
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
    cpu    = 0.5
    memory = "1Gi"
  }

  microservice_container_app = {
    min_replicas = 1
    max_replicas = 1
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
    cpu    = 0.5
    memory = "1Gi"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "UAT"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  key_vault_resource_group_name = "${local.prefix}-${local.env_short}-${local.domain}-sec-rg"
  key_vault_name                = "${local.prefix}-${local.env_short}-${local.domain}-kv"

  project                  = "${local.prefix}-${local.env_short}"
  resource_group_name_vnet = "${local.project}-vnet-rg"
  image_tag                = var.image_tag

}

locals {
  container_app_onboarding_bff = {
    min_replicas = 1
    max_replicas = 2
    scale_rules = [
      {
        custom = {
          metadata = {
            desiredReplicas = "1"
            start           = "0 8 * * MON-FRI"
            end             = "0 19 * * MON-FRI"
            timezone        = "Europe/Rome"
          }
          type = "cron"
        }
        name = "cron-scale-rule"
      }
    ]
    cpu    = 0.5
    memory = "1Gi"
  }

  app_settings_onboarding_bff = [
    { name = "APPLICATIONINSIGHTS_ROLE_NAME", value = "b4f-onboarding" },
    { name = "JAVA_TOOL_OPTIONS", value = "-javaagent:applicationinsights-agent.jar" },
    { name = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", value = "OFF" },
    { name = "B4F_ONBOARDING_LOG_LEVEL", value = "DEBUG" },
    { name = "REST_CLIENT_LOGGER_LEVEL", value = "FULL" },
    { name = "MS_ONBOARDING_URL", value = "http://selc-u-pnpg-onboarding-ms-ca" },
    { name = "MS_CORE_URL", value = "http://selc-u-pnpg-ms-core-ca" },
    { name = "USERVICE_PARTY_PROCESS_URL", value = "http://selc-u-pnpg-ms-core-ca" },
    { name = "USERVICE_PARTY_REGISTRY_PROXY_URL", value = "http://selc-u-pnpg-party-reg-proxy-ca" },
    { name = "USERVICE_USER_REGISTRY_URL", value = "https://api.uat.pdv.pagopa.it/user-registry/v1" },
    { name = "REST_CLIENT_CONNECT_TIMEOUT", value = "60000" },
    { name = "REST_CLIENT_READ_TIMEOUT", value = "60000" },
    { name = "MS_USER_URL", value = "http://selc-u-pnpg-user-ms-ca" },
    { name = "PRODUCT_STORAGE_CONTAINER", value = "selc-u-product" },
    { name = "ONBOARDING_FUNCTIONS_URL", value = "https://selc-u-pnpg-onboarding-fn.azurewebsites.net" },
    { name = "MS_USER_INSTITUTION_URL", value = "https://selc-u-user-ms-ca" },
    { name = "MS_PRODUCT_URL", value = "http://selc-u-product-ms-ca" }
  ]

  secrets_names_onboarding_bff = {
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
    "ONBOARDING-FUNCTIONS-API-KEY"           = "fn-onboarding-primary-key"
  }

  private_dns_name_onboarding_bff    = "selc-u-pnpg-onboarding-bff-ca.orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"
  apim_name_onboarding_bff           = "selc-${local.env_short}-pnpg-apim-v2"
  apim_rg_onboarding_bff             = "selc-${local.env_short}-pnpg-api-v2-rg"
  dns_zone_prefix_onboarding_bff     = "imprese.uat.notifichedigitali"
  api_dns_zone_prefix_onboarding_bff = "api-pnpg.uat.selfcare"
}
