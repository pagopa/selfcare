env_short           = "p"
is_pnpg             = true
dns_zone_prefix     = "pnpg.selfcare"
api_dns_zone_prefix = "api-pnpg.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/webhook"
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
    value = "webhook-ms",
  },
  {
    name  = "MONGODB_DATABASE_NAME"
    value = "selcWebhook"
  }
]

secrets_names = {
  "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
  "JWT_PUBLIC_KEY"                        = "jwt-public-key"
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
}
