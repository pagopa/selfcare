###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "prod"
  env_short = "p"
  domain    = "ar"

  dns_zone_prefix                = "selfcare"
  api_dns_zone_prefix            = "api.selfcare"
  private_dns_name_domain        = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-p-cae-002"
  ca_resource_group_name         = "selc-p-container-app-002-rg"
}

###############################################################################
# Institution Send Mail Scheduler Container App Job
###############################################################################
locals {
  image_tag = var.image_tag

  app_settings_institution_send_mail_scheduler = [
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
      value = "sc-p-documents-blob"
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
      value = "selc-p-product"
    },
    {
      name  = "MAIL_DESTINATION_TEST"
      value = "false"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "http://selc-p-user-ms-ca"
    },
    {
      name  = "SEND_ALL_NOTIFICATION"
      value = "true"
    }
  ]

  secrets_names_institution_send_mail_scheduler = {
    "MONGODB_CONNECTION_STRING"               = "mongodb-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
    "BLOB_STORAGE_CONN_STRING_PRODUCT"        = "blob-storage-product-connection-string"
    "MAIL_SERVER_USERNAME"                    = "smtp-usr"
    "MAIL_SENDER_ADDRESS"                     = "smtp-usr"
    "MAIL_SERVER_PASSWORD"                    = "smtp-psw"
    "JWT_BEARER_TOKEN"                        = "jwt-bearer-token-functions"
  }
}

module "container_app_job_institution_send_mail_scheduler" {
  source = "../../_modules/container_app_job"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-inst-send-mail"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-institution-send-mail-scheduler"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_institution_send_mail_scheduler
  secrets_names                  = local.secrets_names_institution_send_mail_scheduler
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags

  schedule_trigger_config = [{
    cron_expression          = "00 06 * * *"
    parallelism              = 1
    replica_completion_count = 1
  }]

  replica_timeout_in_seconds = 28800
}
