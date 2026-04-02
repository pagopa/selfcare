###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
}

###############################################################################
# APIM
###############################################################################

module "apim_api_auth" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-api-auth"
  display_name        = "Auth API"
  base_path           = "auth"
  private_dns_name    = "selc-${module.local.config.env_short}-auth-ms-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  openapi_path        = "../../../../apps/auth/src/main/docs/openapi.json"

  api_operation_policies = [{
    operation_id = "loginSaml"
    xml_content  = <<XML
      <policies>
          <inbound>
              <cors allow-credentials="true">
                  <allowed-origins>
                      <origin>https://${module.local.config.dns_zone_prefix}.${module.local.config.external_domain}</origin>
                      <origin>https://${module.local.config.api_dns_zone_prefix}.${module.local.config.external_domain}</origin>
                      <origin>http://localhost:3000</origin>
                      <origin>https://accounts.google.com</origin>
                  </allowed-origins>
                  <allowed-methods>
                      <method>POST</method>
                  </allowed-methods>
                  <allowed-headers>
                      <header>*</header>
                  </allowed-headers>
              </cors>
              <base />
          </inbound>
          <backend>
              <base />
          </backend>
          <outbound>
              <base />
          </outbound>
          <on-error>
              <base />
          </on-error>
      </policies>
      XML
    }
  ]
}

###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_auth" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcAuth"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_auth_otp_flows" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "otpFlows"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb_auth.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["uuid"], unique = true },
    { keys = ["userId"], unique = false },
    { keys = ["expiresAt"], unique = false },
    { keys = ["status"], unique = false },
    { keys = ["userId", "createdAt"], unique = false },
    { keys = ["createdAt"], unique = false }
  ]

  depends_on = [module.cosmosdb_auth]
}

###############################################################################
# Container App
###############################################################################

locals {
  app_settings_auth_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "auth-ms"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "AUTH_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "AUTH_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "AUTH_MS_RETRY"
      value = 3
    },
    {
      name  = "SESSION_TOKEN_DURATION_HOURS"
      value = 9
    },
    {
      name  = "SESSION_TOKEN_AUDIENCE"
      value = "api.dev.selfcare.pagopa.it"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ONE_IDENTITY_URL"
      value = "https://uat.oneid.pagopa.it"
    },
    {
      name  = "FEATURE_FLAG_OTP_ENABLED"
      value = "BETA"
    },
    {
      name  = "INTERNAL_API_URL"
      value = "https://api.dev.selfcare.pagopa.it/external/internal/v1"
    },
    {
      name  = "INTERNAL_MS_USER_API_URL"
      value = "https://api.dev.selfcare.pagopa.it/internal/user"
    },
    {
      name  = "SAML_SP_ACS_URL"
      value = "https://dev.selfcare.pagopa.it/saml/acs"
    },
    {
      name  = "SAML_SP_ENTITY_ID"
      value = "https://dev.selfcare.pagopa.it"
    },
    {
      name  = "IAM_API_URL"
      value = "https://selc-d-iam-ms-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
    },
    {
      name  = "OTP_DAILY_LIMIT"
      value = 0
    }
  ]

  secrets_names_auth_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "ONE_IDENTITY_CLIENT_ID"                = "oneidentity-client-id"
    "ONE_IDENTITY_CLIENT_SECRET"            = "oneidentity-client-secret"
    "SESSION_TOKEN_PRIVATE_KEY"             = "jwt-private-key-pkcs8"
    "USER-REGISTRY-API-KEY"                 = "user-registry-api-key"
    "INTERNAL-API-KEY"                      = "internal-api-key"
    "INTERNAL-MS-USER-API-KEY"              = "internal-ms-user-api-key"
    "FEATURE_FLAG_OTP_BETA_USERS"           = "feature-flag-otp-beta-users"
    "SAML_IDP_ENTITY_ID"                    = "saml-idp-entity-id"
    "SAML_IDP_METADATA"                     = "saml-idp-metadata"
    "SAML_IDP_CERT"                         = "saml-idp-cert"
  }
}
module "container_app_auth_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-auth-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-auth-ms"
  image_tag                      = module.local.config.image_tag_latest
  app_settings                   = local.app_settings_auth_ms
  secrets_names                  = local.secrets_names_auth_ms
  workload_profile_name          = "Consumption"

  key_vault_resource_group_name = module.local.config.key_vault_resource_group_name
  key_vault_name                = module.local.config.key_vault_name

  probes = module.local.config.quarkus_health_probes

  tags = module.local.config.tags
}
