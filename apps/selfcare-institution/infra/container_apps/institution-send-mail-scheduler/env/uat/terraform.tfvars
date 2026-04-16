prefix           = "selc"
env_short        = "u"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-institution"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 1
  scale_rules  = []
  cpu          = 1
  memory       = "2Gi"
}

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "institution_send_mail_scheduler",
  },
  {
    name  = "STORAGE_CONTAINER_CONTRACT"
    value = "sc-u-documents-blob"
  },
  {
    name  = "MAIL_DESTINATION_TEST_ADDRESS"
    value = "pectest@pec.pagopa.it"
  },
  {
    name  = "MAIL_SERVER_HOST"
    value = "smtps.pec.aruba.it"
  },
  {
    name  = "MAIL_SERVER_PORT"
    value = "465"
  },
  {
    name  = "MAIL_TEMPLATE_NOTIFICATION_PATH"
    value = "contracts/template/mail/institution-user-list-notification/1.0.2.json"
  },
  {
    name  = "MAIL_TEMPLATE_FIRST_NOTIFICATION_PATH"
    value = "contracts/template/mail/institution-user-list-first-notification/1.0.2.json"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-u-product"
  },
  {
    name  = "SELFCARE_USER_URL"
    value = "http://selc-u-user-ms-ca"
  },
  {
    name  = "SEND_ALL_NOTIFICATION"
    value = "false"
  }
]


secrets_names = {
  "MONGODB_CONNECTION_STRING"               = "mongodb-connection-string"
  "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
  "BLOB_STORAGE_CONN_STRING_PRODUCT"        = "blob-storage-product-connection-string"
  "MAIL_SERVER_USERNAME"                    = "smtp-usr"
  "MAIL_SENDER_ADDRESS"                     = "smtp-usr"
  "MAIL_SERVER_PASSWORD"                    = "smtp-psw"
  "JWT_BEARER_TOKEN"                        = "jwt-bearer-token-functions"
}