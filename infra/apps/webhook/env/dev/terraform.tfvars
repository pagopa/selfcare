env_short           = "d"
suffix_increment    = "-002"
cae_name            = "cae-002"
dns_zone_prefix     = "dev.selfcare"
api_dns_zone_prefix = "api.dev.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/webhook"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

private_dns_name = "https://selc-d-webhook-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

container_app = {
  min_replicas = 0
  max_replicas = 1
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "1"
          "start"           = "0 8 * * *"
          "end"             = "0 20 * * *"
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

workload_profile_name = "Consumption"

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "webhook-ms",
  },
  {
    name  = "MONGODB_DATABASE_NAME"
    value = "selcWebhook"
  }
]

secrets_names = {
  "MONGODB_CONNECTION_STRING" = "mongodb-connection-string"
  "JWT_PUBLIC_KEY"            = "jwt-public-key"
}
