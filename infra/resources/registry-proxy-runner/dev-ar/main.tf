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
  container_app_cpu              = 1.25
  container_app_memory           = "2.5Gi"
}

###############################################################################
# Registry Proxy Runner Container App Job
###############################################################################
data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${module.local.config.container_app_environment_name}-managed_identity"
  resource_group_name = module.local.config.ca_resource_group_name
}

locals {
  image_tag = var.image_tag

  blob_storage_account_name = "selc${module.local.config.env_short}${module.local.config.location_short}archeckoutst01"

  app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "registry_proxy_runner",
    },
    {
      name  = "AZURE_SEARCH_BASE_URL"
      value = "https://selc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-srch.search.windows.net"
    },
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME"
      value = local.blob_storage_account_name
    },
    {
      # Required for DefaultAzureCredential to use the correct user-assigned managed identity
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.cae_identity.client_id
    }
  ]

  secrets = {
    "AZURE_SEARCH_API_KEY"          = "azure-search-api-key"
    "APPINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  }
}

module "container_app" {
  source = "../../_modules/container_app_job"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-reg-proxy-runner"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-registry-proxy-runner-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags

  schedule_trigger_config = [{
    cron_expression          = "0 */6 * * *"
    parallelism              = 1
    replica_completion_count = 1
  }]

  replica_timeout_in_seconds = 28800
}
