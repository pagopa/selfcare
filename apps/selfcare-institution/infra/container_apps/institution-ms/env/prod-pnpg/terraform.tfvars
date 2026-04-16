is_pnpg   = true
env_short = "p"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-institution"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 3
  max_replicas = 5
  scale_rules = [
    {
      custom = {
        metadata = {
          concurrentRequests = "30"
        }
        type = "http"
      }
      name = "http-scale-rule"
    }
  ]
  cpu    = 1.25
  memory = "2.5Gi"
}

app_settings = [
  {
    name  = "SELFCARE_URL"
    value = "https://selfcare.pagopa.it"
  },
  {
    name  = "LOGO_URL"
    value = "https://pnpg.selfcare.pagopa.it/institutions/"
  },
  {
    name  = "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"
    value = "contracts/template/mail/delegation-notification/1.0.1.json"
  },
  {
    name  = "MAIL_TEMPLATE_DELEGATION_USER_NOTIFICATION_PATH"
    value = "contracts/template/mail/delegation-notification/user-1.0.1.json"
  },
  {
    name  = "STORAGE_CONTAINER"
    value = "$web"
  },
  {
    name  = "STORAGE_ENDPOINT"
    value = "core.windows.net"
  },

  {
    name  = "STORAGE_APPLICATION_ID"
    value = "selcpweupnpgcheckoutsa"
  },

  {
    name  = "STORAGE_CREDENTIAL_ID"
    value = "selcpweupnpgcheckoutsa"
  },
  {
    name  = "STORAGE_TEMPLATE_URL"
    value = "https://selcpweupnpgcheckoutsa.z6.web.core.windows.net"
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "institution-ms"
  },
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar"
  },
  {
    name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
    value = "OFF"
  },
  {
    name  = "EXTERNAL_API_LOG_LEVEL"
    value = "INFO"
  },
  {
    name  = "SMTP_HOST"
    value = "smtps.pec.aruba.it"
  },
  {
    name  = "SMTP_PORT"
    value = "465"
  },
  {
    name  = "SMTP_SSL"
    value = "true"
  },
  {
    name  = "MS_NOTIFICATION_MANAGER_URL"
    value = "http://selc-p-pnpg-notification-mngr-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-p-pnpg-party-reg-proxy-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "SELFCARE_USER_URL"
    value = "http://selc-p-pnpg-user-ms-ca"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-p-product"
  },
  {
    name  = "MAIL_SENDER_ADDRESS"
    value = "noreply@areariservata.pagopa.it"
  },
  {
    name  = "PEC_NOTIFICATION_DISABLED"
    value = "true"
  }
]


secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"    = "appinsights-connection-string"
  "MONGODB_CONNECTION_URI"                   = "mongodb-connection-string"
  "BLOB_STORAGE_CONN_STRING"                 = "blob-storage-contract-connection-string"
  "SMTP_USR"                                 = "smtp-usr"
  "SMTP_PSW"                                 = "smtp-psw"
  "ONBOARDING_INSTITUTION_ALTERNATIVE_EMAIL" = "party-test-institution-email"
  "USER_REGISTRY_API_KEY"                    = "user-registry-api-key"
  "JWT_TOKEN_PUBLIC_KEY"                     = "jwt-public-key"
  "BLOB_STORAGE_PRODUCT_CONNECTION_STRING"   = "blob-storage-product-connection-string"
  "AWS_SES_ACCESS_KEY_ID"                    = "aws-ses-access-key-id"
  "AWS_SES_SECRET_ACCESS_KEY"                = "aws-ses-secret-access-key"
}
