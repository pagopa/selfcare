env_short = "d"
is_pnpg   = true
dns_zone_prefix     = "pnpg.dev.selfcare"
api_dns_zone_prefix = "api-pnpg.dev.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/auth"
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
  cpu    = 0.5
  memory = "1Gi"
}

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "pnpg-auth-ms",
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
    name = "SESSION_TOKEN_DURATION_HOURS"
    value = 9
  },
  {
    name = "SESSION_TOKEN_AUDIENCE"
    value = "api-pnpg.dev.selfcare.pagopa.it"
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
    value = "NONE"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "ONE_IDENTITY_CLIENT_ID"                = "oneidentity-client-id"
  "ONE_IDENTITY_CLIENT_SECRET"            = "oneidentity-client-secret"
  "SESSION_TOKEN_PRIVATE_KEY"             = "jwt-private-key"
  "USER-REGISTRY-API-KEY"                 = "user-registry-api-key"
}

