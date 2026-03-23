env_short                       = "d"
private_dns_name                = "selc-d-party-reg-proxy-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
dns_zone_prefix                 = "dev.selfcare"
api_dns_zone_prefix             = "api.dev.selfcare"
suffix_increment                = "-002"
cae_name                        = "cae-002"
cae_rg_name                     = "selc-d-container-app-002-rg"
key_vault_name                  = "selc-d-kv"
key_vault_resource_group_name   = "selc-d-sec-rg"
existing_logs_rg                = "selc-d-logs-storage-rg"
ca_name                         = "selc-d-party-reg-proxy-ca"
ca_rg_name                      = "selc-d-container-app-002-rg"
storage_logs                    = "selcdstlogs"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-ms-party-registry-proxy"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 0
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
  cpu    = 1
  memory = "2Gi"
}

app_settings = [
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
    name = "PDND_SKIP_LOCALIZZAZIONE_NODES"
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
