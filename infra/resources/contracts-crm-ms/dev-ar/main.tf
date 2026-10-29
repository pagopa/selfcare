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
}

###############################################################################
# CosmosDB
###############################################################################
locals {
  database_name = "selcContractsCrm"
}

module "cosmosdb_contracts_crm" {
  source = "../../_modules/cosmosdb_database"

  database_name               = local.database_name
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_contract_delegations" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "contractDelegations"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb_contracts_crm.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    # One contract delegation per product and delegating PSP contract.
    { keys = ["productId", "contract.id"], unique = true }
  ]
}

###############################################################################
# Contracts CRM Container App
# Receives SC-Delegations notifications from webhook-ms and publishes on the
# SC-ContractsCRMDelegations Event Hub topic. Not exposed through APIM.
###############################################################################
locals {
  app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "contracts-crm-ms"
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = local.database_name
    },
    {
      name  = "ONBOARDING_URL"
      value = "https://selc-${module.local.config.env_short}-onboarding-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "WEBHOOK_URL"
      value = "https://selc-${module.local.config.env_short}-webhook-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-${module.local.config.env_short}-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_CONTRACTS_CRM_DELEGATIONS_TOPIC"
      value = "sc-contractscrmdelegations"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    }
  ]

  secrets_names = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"                  = "appinsights-connection-string"
    "MONGODB_CONNECTION_STRING"                              = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                                         = "jwt-public-key"
    "JWT_TOKEN_PRIVATE_KEY"                                  = "jwt-private-key-pkcs8"
    "JWT_TOKEN_KID"                                          = "jwt-kid"
    "EVENTHUB_SC_CONTRACTSCRMDELEGATIONS_SELFCARE_WO_KEY_LC" = "eventhub-sc-contractscrmdelegations-selfcare-wo-key-lc"
  }
}

module "container_app" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-contracts-crm-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-contracts-crm-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
