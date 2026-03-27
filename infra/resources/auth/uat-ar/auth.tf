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
      value = "api.uat.selfcare.pagopa.it"
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
      value = "https://api.uat.selfcare.pagopa.it/external/internal/v1"
    },
    {
      name  = "INTERNAL_MS_USER_API_URL"
      value = "https://api.uat.selfcare.pagopa.it/internal/user"
    },
    {
      name  = "SAML_SP_ACS_URL"
      value = "https://uat.selfcare.pagopa.it/saml/acs"
    },
    {
      name  = "SAML_SP_ENTITY_ID"
      value = "https://uat.selfcare.pagopa.it"
    },
    {
      name  = "IAM_API_URL"
      value = "https://selc-u-iam-ms-ca.mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
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

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.microservice_container_app
  container_app_name             = "${local.project}-auth-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-auth-ms"
  image_tag                      = local.auth_image_tag
  app_settings                   = local.app_settings_auth_ms
  secrets_names                  = local.secrets_names_auth_ms

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}
