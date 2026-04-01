###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-pnpg"
}
locals {
  ca_name          = "selc-${module.local.config.env_short}-party-reg-proxy-ca"

  registry_proxy_app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
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
      value = "https://selcdcheckoutsa.z6.web.core.windows.net/resources"
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
      value = "https://selc-d-ms-core-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
    },
    {
      name  = "AZURE_SEARCH_URL"
      value = "https://selc-d-weu-ar-srch.search.windows.net/"
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
      value = "selc-d-redis.redis.cache.windows.net"
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

  app_settings = local.registry_proxy_app_settings

  cae_id               = try(data.azurerm_container_app_environment.cae.id, null)
  container_app_id     = try(data.azurerm_container_app.ca.id, null)
  storage_account_id   = try(data.azurerm_storage_account.existing_logs_storage.id, null)
  storage_account_name = try(data.azurerm_storage_account.existing_logs_storage.name, null)
  key_vault_id         = try(data.azurerm_key_vault.key_vault.id, null)
  logs_storage_key     = try(data.azurerm_key_vault_secret.logs_storage_access_key.value, null)
}


###############################################################################
# Container App
###############################################################################

module "container_app_registry_proxy_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = local.ca_name //"party-reg-proxy"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-ms-party-registry-proxy"
  image_tag                      = module.local.config.image_tag_latest
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = "Consumption"

  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name

  probes = module.local.config.quarkus_health_probes
  tags   = module.local.config.tags
}
