env_short           = "p"
suffix_increment    = "-002"
cae_name            = "cae-002"
dns_zone_prefix     = "selfcare"
api_dns_zone_prefix = "api.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/auth"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

private_dns_name = "selc-p-auth-ms-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io"

container_app = {
  min_replicas = 1
  max_replicas = 5
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "3"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 19 * * MON-FRI"
          "timezone"        = "Europe/Rome"
        }
        type = "cron"
      }
      name = "cron-scale-rule"
    }
  ]
  cpu    = 1.25
  memory = "2.5Gi"
}

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "auth-ms",
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
    value = "api.selfcare.pagopa.it"
  },
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONE_IDENTITY_URL"
    value = "https://oneid.pagopa.it"
  },
  {
    name  = "FEATURE_FLAG_OTP_ENABLED"
    value = "ALL"
  },
  {
    name  = "INTERNAL_API_URL"
    value = "https://api.selfcare.pagopa.it/external/internal/v1"
  },
  {
    name  = "INTERNAL_MS_USER_API_URL"
    value = "https://api.selfcare.pagopa.it/internal/user"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
  "ONE_IDENTITY_CLIENT_ID"                = "oneidentity-client-id"
  "ONE_IDENTITY_CLIENT_SECRET"            = "oneidentity-client-secret"
  "SESSION_TOKEN_PRIVATE_KEY"             = "jwt-private-key-pkcs8"
  "USER-REGISTRY-API-KEY"                 = "user-registry-api-key"
  "INTERNAL-API-KEY"                      = "internal-api-key"
  "INTERNAL-MS-USER-API-KEY"              = "internal-ms-user-api-key"
  "FEATURE_FLAG_OTP_BETA_USERS"           = "feature-flag-otp-beta-users"
}
