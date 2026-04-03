###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-uat-ar"
}


###############################################################################
# Container App
###############################################################################


# ONLY FOR AR
module "apim_api_registry_proxy" {
  source              = "../../_modules/apim_api"
  apim_name           = "selc-${module.local.config.env_short}-apim-v2"
  apim_rg             = "selc-${module.local.config.env_short}-api-v2-rg"
  api_name            = "selc-${module.local.config.env_short}-api-bff-proxy"
  display_name        = "BFF Proxy API"
  base_path           = "party-registry-proxy/v1"
  private_dns_name    = "selc-${module.local.config.env_short}-party-reg-proxy-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  openapi_path        = "../../../../apps/registry-proxy/app/src/main/resources/swagger/apim_api_bff_proxy.json"
}


###############################################################################
# DAPR
###############################################################################
locals {
  ca_base_name    = "selc-${module.local.config.env_short}-party-reg-proxy"
  ca_name         = "${local.ca_base_name}-ca"
  storage_logs    = "selc${module.local.config.env_short}stlogs"
  storage_logs_rg = "selc-${module.local.config.env_short}-logs-storage-rg"

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

  dapr_settings = [{
    name  = "DAPR_HTTP_PORT"
    value = "3500"
    },
    {
      name  = "DAPR_GRPC_PORT"
      value = "50001"
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = module.container_app_registry_proxy_ms.cae_identity_id
    }
  ]

  dapr_sidecar_settings = [
    {
      app_id       = "party-reg-proxy"
      app_port     = 8080
      app_protocol = "http"
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
      value = "https://selc${module.local.config.env_short}checkoutsa.z6.web.core.windows.net/resources"
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
      name  = "ANAC_FTP_IP"
      value = "93.43.119.85"
    },
    {
      name  = "ANAC_FTP_USER"
      value = "PagoPA_user"
    },
    {
      name  = "ANAC_FTP_DIRECTORY"
      value = "/mnt/RegistroGestoriPiattaforme/Collaudo/"
    },
    {
      name  = "LUCENE_INDEX_INSTITUTIONS_FOLDER"
      value = "index/institutions"
    },
    {
      name  = "LUCENE_INDEX_CATEGORIES_FOLDER"
      value = "index/categories"
    },
    {
      name  = "LUCENE_INDEX_AOOS_FOLDER"
      value = "index/aoos"
    },
    {
      name  = "LUCENE_INDEX_UOS_FOLDER"
      value = "index/uos"
    },
    {
      name  = "LUCENE_INDEX_ANAC_FOLDER"
      value = "index/anac"
    },
    {
      name  = "LUCENE_INDEX_IVASS_FOLDER"
      value = "index/ivass"
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
      name  = "SELC_INSTITUTION_URL"
      value = "https://selc-${module.local.config.env_short}-ms-core-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "AZURE_SEARCH_URL"
      value = "https://selc-${module.local.config.env_short}-weu-ar-srch.search.windows.net/"
    },
    {
      name  = "AZURE_SEARCH_INSTITUTION_INDEX"
      value = "institution-index-ar"
    },
    {
      name  = "ANAC_FTP_MODE"
      value = "azure"
    },
    {
      name  = "REDIS_URL"
      value = "selc-${module.local.config.env_short}-redis.redis.cache.windows.net"
    },
    {
      name  = "REDIS_PORT"
      value = "6380"
    },
    {
      name  = "PDND_SKIP_LOCALIZZAZIONE_NODES"
      value = "false"
    }
  ]

  secrets_names = {
    "BLOB_STORAGE_CONN_STRING"              = "web-storage-connection-string"
    "NATIONAL_REGISTRY_API_KEY"             = "national-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                  = "jwt-public-key"
    "GEOTAXONOMY_API_KEY"                   = "geotaxonomy-api-key"
    "ANAC_FTP_PASSWORD"                     = "anac-ftp-password"
    "ANAC_FTP_KNOWN_HOST"                   = "anac-ftp-known-host"
    "PDND_INFOCAMERE_PRIVATE_KEY"           = "infocamere-interop-certificate-client-private-key"
    "PDND_INFOCAMERE_CLIENT_ID"             = "infocamere-interop-client-id"
    "PDND_INFOCAMERE_KID"                   = "infocamere-interop-kid"
    "PDND_INFOCAMERE_PURPOSE_ID"            = "infocamere-interop-purpose-id"
    "PDND_INVITALIA_INFOCAMERE_PRIVATE_KEY" = "invitalia-interop-certificate-client-private-key"
    "PDND_INVITALIA_INFOCAMERE_CLIENT_ID"   = "invitalia-interop-client-id"
    "PDND_INVITALIA_INFOCAMERE_KID"         = "invitalia-interop-kid"
    "PDND_INVITALIA_INFOCAMERE_PURPOSE_ID"  = "invitalia-interop-purpose-id"
    "JWT-BEARER-TOKEN-FUNCTIONS"            = "jwt-bearer-token-functions"
    "AZURE_SEARCH_API_KEY"                  = "azure-search-api-key"
    "APPINSIGHTS_CONNECTION_STRING"         = "appinsights-connection-string"
    "ONBOARDING_DATA_ENCRIPTION_KEY"        = "onboarding-data-encryption-key"
    "ONBOARDING_DATA_ENCRIPTION_IV"         = "onboarding-data-encryption-iv"
    "REDIS_PASSWORD"                        = "redis-primary-access-key"
  }

  app_settings = concat(local.registry_proxy_app_settings, local.dapr_settings)

  # cae_id               = 
  # container_app_id     = try(data.azurerm_container_app.ca.id, null)
  # storage_account_id   = try(data.azurerm_storage_account.existing_logs_storage.id, null)
  # storage_account_name = 
  # key_vault_id         = try(data.azurerm_key_vault.key_vault.id, null)
  # logs_storage_key     = try(data.azurerm_key_vault_secret.logs_storage_access_key.value, null)
}

resource "azurerm_storage_container" "visura" {
  name                  = "visura"
  storage_account_id    = try(data.azurerm_storage_account.existing_logs_storage.id, null)
  container_access_type = "private"
}

resource "azurerm_container_app_environment_dapr_component" "blob_state" {

  name                         = "blobstorage-state"
  container_app_environment_id = try(data.azurerm_container_app_environment.cae.id, null)
  component_type               = "state.azure.blobstorage"
  version                      = "v1"

  metadata {
    name  = "accountName"
    value = try(data.azurerm_storage_account.existing_logs_storage.name, null)
  }

  metadata {
    name  = "containerName"
    value = try(azurerm_storage_container.visura.name, null)
  }

  metadata {
    name  = "azureClientId"
    value = module.container_app_registry_proxy_ms.cae_identity_id
  }

  scopes = [local.ca_name]

  lifecycle {
    prevent_destroy = false
  }
}

###############################################################################
# Container App
###############################################################################

module "container_app_registry_proxy_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = local.registry_proxy_container_app
  container_app_name             = local.ca_base_name
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-ms-party-registry-proxy"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = "Consumption"
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name

  dapr_settings = local.dapr_sidecar_settings
  probes        = local.spring_boot_health_probes
  tags   = module.local.config.tags
}

###############################################################################
# DAPR
###############################################################################


module "dapr" {
  source = "../../_modules/dapr"

  project   = "${module.local.config.prefix}-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}"
  env_short = module.local.config.env_short

  cae_name    = "selc-${module.local.config.env_short}-cae-002"
  cae_rg_name = module.local.config.ca_resource_group_name
  ca_name     = local.ca_name
  ca_rg_name  = "selc-${module.local.config.env_short}-container-app-002-rg"

  key_vault_name                   = "selc-${module.local.config.env_short}-kv"
  key_vault_resource_group_name    = "selc-${module.local.config.env_short}-sec-rg"
  key_vault_event_hub_consumer_key = "eventhub-sc-contracts-selc-proxy-key-lc"

  queue_url            = "selc-${module.local.config.env_short}-eventhub-ns.servicebus.windows.net"
  queue_port           = "9093"
  queue_consumer_group = "party-proxy"
  queue_topic          = "SC-Contracts"

  #redis
  redis_enable                   = true
  redis_private_endpoint_enabled = true
  redis_capacity                 = 0
  redis_version                  = 6
  redis_family                   = "C"
  redis_sku_name                 = "Basic"

  search_service_name = data.azurerm_search_service.srch_service.name
  search_service_key  = data.azurerm_search_service.srch_service.primary_key

  tags = module.local.config.tags
}
