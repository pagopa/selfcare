env_short           = "p"
suffix_increment    = "-002"
cae_name            = "cae-002"
dns_zone_prefix     = "selfcare"
api_dns_zone_prefix = "api.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/iam"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

private_dns_name = "TOEDIT"

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
    value = "iam-ms",
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  },
  {
    name  = "IAM_MS_RETRY_MIN_BACKOFF"
    value = 5
  },
  {
    name  = "IAM_MS_RETRY_MAX_BACKOFF"
    value = 60
  },
  {
    name  = "IAM_MS_RETRY"
    value = 3
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
}
