prefix    = "selc"
env_short = "u"
location  = "westeurope"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-u-pnpg-sec-rg"
  name                = "selc-u-pnpg-kv"
}


cidr_subnet_selc_onboarding_fn = ["10.1.152.0/24"]

function_always_on = true

app_service_plan_info = {
  kind                         = "Linux"
  sku_size                     = "B2"
  maximum_elastic_worker_count = 1
  worker_count                 = 1
  zone_balancing_enabled       = false
}

storage_account_info = {
  account_kind                      = "StorageV2"
  account_tier                      = "Standard"
  account_replication_type          = "LRS"
  access_tier                       = "Hot"
  advanced_threat_protection_enable = false
  use_legacy_defender_version       = true
  public_network_access_enabled     = false
}

app_settings = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/appinsights-connection-string/)",
  "USER_REGISTRY_URL"                     = "https://api.uat.pdv.pagopa.it/user-registry/v1",
  "MONGODB_CONNECTION_URI"                = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/mongodb-connection-string/)",
  "USER_REGISTRY_API_KEY"                 = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/user-registry-api-key/)",
  "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)",
  "STORAGE_CONTAINER_PRODUCT"             = "selc-u-product",

  ## PNPG contains template mail in checkout storage
  "BLOB_STORAGE_CONN_STRING_CONTRACT" = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)",
  "STORAGE_CONTAINER_CONTRACT"        = "$web",

  "MAIL_DESTINATION_TEST_ADDRESS" = "pectest@pec.pagopa.it",
  "MAIL_SENDER_ADDRESS"           = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_USERNAME"          = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_PASSWORD"          = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/smtp-psw/)",
  "MAIL_SERVER_HOST"              = "smtps.pec.aruba.it",
  "MAIL_SERVER_PORT"              = "465",
  "MAIL_TEMPLATE_COMPLETE_PATH"   = "resources/templates/email/onboarding_1.0.0.json",

  "MS_USER_URL"                                  = "https://selc-u-pnpg-user-ms-ca.orangeground-0bd2d4dc.westeurope.azurecontainerapps.io",
  "MS_CORE_URL"                                  = "https://selc-u-pnpg-ms-core-ca.orangeground-0bd2d4dc.westeurope.azurecontainerapps.io",
  "JWT_BEARER_TOKEN"                             = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)",
  "MS_PARTY_REGISTRY_URL"                        = "https://selc-u-pnpg-party-reg-proxy-ca.orangeground-0bd2d4dc.westeurope.azurecontainerapps.io",
  "PAGOPA_LOGO_ENABLE"                           = "false",
  "RETRY_MAX_ATTEMPTS"                           = "3"
  "FIRST_RETRY_INTERVAL"                         = "5"
  "BACKOFF_COEFFICIENT"                          = "1"
  "EVENT_HUB_BASE_PATH"                          = "https://selc-u-eventhub-ns.servicebus.windows.net",
  "STANDARD_SHARED_ACCESS_KEY_NAME"              = "selfcare-wo"
  "EVENTHUB_SC_CONTRACTS_SELFCARE_WO_KEY_LC"     = "string"
  "STANDARD_TOPIC_NAME"                          = "SC-Contracts"
  "SAP_SHARED_ACCESS_KEY_NAME"                   = "external-interceptor-wo"
  "EVENTHUB_SC_CONTRACTS_SAP_SELFCARE_WO_KEY_LC" = "string"
  "SAP_TOPIC_NAME"                               = "SC-Contracts-SAP"
  "FD_SHARED_ACCESS_KEY_NAME"                    = "external-interceptor-wo"
  "EVENTHUB_SC_CONTRACTS_FD_SELFCARE_WO_KEY_LC"  = "string"
  "FD_TOPIC_NAME"                                = "Selfcare-FD"
  "SAP_ALLOWED_INSTITUTION_TYPE"                 = "PA,GSP,SA,AS,SCP"
  "SAP_ALLOWED_ORIGINS"                          = "IPA,SELC"
  "MINUTES_THRESHOLD_FOR_UPDATE_NOTIFICATION"    = "5"

  ## IGNORE VALUES

  "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json",
  "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json",
  "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/portal-admin-operator-email/)",
  "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json",
  "MAIL_TEMPLATE_AGGREGATE_COMPLETE_PATH"              = "contracts/template/mail/onboarding-complete-aggregate/1.0.0.json",
  "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json",
  "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/onboarding-request/1.0.1.json",
  "MAIL_TEMPLATE_REGISTRATION_AGGREGATOR_PATH"         = "contracts/template/mail/onboarding-request-aggregator/1.0.1.json"
  "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.0.json",
  "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_USER_PATH"               = "contracts/template/mail/onboarding-request-admin/1.0.0.json",
  "MAIL_TEMPLATE_USER_COMPLETE_NOTIFICATION_PATH"      = "contracts/template/mail/onboarding-complete-user/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_USER_NEW_MANAGER_PATH"   = "contracts/template/mail/onboarding-request-manager/1.0.0.json",
  "SELFCARE_ADMIN_NOTIFICATION_URL"                    = "https://imprese.uat.notifichedigitali.it/dashboard/admin/onboarding/",
  "SELFCARE_URL"                                       = "https://imprese.uat.notifichedigitali.it",
  "MAIL_ONBOARDING_CONFIRMATION_LINK"                  = "https://imprese.uat.notifichedigitali.it/onboarding/confirm?jwt=",
  "MAIL_USER_CONFIRMATION_LINK"                        = "https://imprese.uat.notifichedigitali.it/onboarding/confirm?add-user=true&jwt=",
  "MAIL_ONBOARDING_REJECTION_LINK"                     = "https://imprese.uat.notifichedigitali.it/onboarding/cancel?jwt=",
  "MAIL_ONBOARDING_URL"                                = "https://imprese.uat.notifichedigitali.it/onboarding/",
  "USER_MS_SEND_MAIL"                                  = "false",
  "FORCE_INSTITUTION_PERSIST"                          = "true",
  "JWT_TOKEN_ISSUER"                                   = "SPID"
  "JWT_TOKEN_PRIVATE_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/jwt-private-key/)"
  "JWT_TOKEN_KID"                                      = "@Microsoft.KeyVault(SecretUri=https://selc-u-pnpg-kv.vault.azure.net/secrets/jwt-kid/)"


}