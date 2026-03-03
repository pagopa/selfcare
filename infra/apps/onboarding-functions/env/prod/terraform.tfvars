prefix    = "selc"
env_short = "p"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-p-sec-rg"
  name                = "selc-p-kv"
}


cidr_subnet_selc_onboarding_fn = ["10.1.144.0/24"]

function_always_on = true

app_service_plan_info = {
  kind                         = "Linux"
  sku_size                     = "P1v3"
  maximum_elastic_worker_count = 1
  worker_count                 = 1
  zone_balancing_enabled       = false
}

storage_account_info = {
  account_kind                      = "StorageV2"
  account_tier                      = "Standard"
  account_replication_type          = "LRS"
  access_tier                       = "Hot"
  advanced_threat_protection_enable = true
  use_legacy_defender_version       = true
  public_network_access_enabled     = false
}

app_settings = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"              = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/appinsights-connection-string/)",
  "USER_REGISTRY_URL"                                  = "https://api.pdv.pagopa.it/user-registry/v1",
  "MONGODB_CONNECTION_URI"                             = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/mongodb-connection-string/)",
  "USER_REGISTRY_API_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/user-registry-api-key/)",
  "BLOB_STORAGE_CONN_STRING_PRODUCT"                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/web-storage-blob-connection-string/)",
  "STORAGE_CONTAINER_CONTRACT"                         = "sc-p-documents-blob",
  "STORAGE_CONTAINER_PRODUCT"                          = "selc-p-product",
  "BLOB_STORAGE_CONN_STRING_CONTRACT"                  = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/documents-storage-connection-string/)",
  "MAIL_DESTINATION_TEST"                              = "false",
  "MAIL_DESTINATION_TEST_ADDRESS"                      = "pectest@pec.pagopa.it",
  "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json",
  "MAIL_SENDER_ADDRESS"                                = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_USERNAME"                               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_PASSWORD"                               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-psw/)",
  "MAIL_SERVER_HOST"                                   = "smtps.pec.aruba.it",
  "MAIL_SERVER_PORT"                                   = "465",
  "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json",
  "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json",
  "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/portal-admin-operator-email/)",
  "MAIL_TEMPLATE_COMPLETE_PATH"                        = "contracts/template/mail/onboarding-complete/1.0.0.json",
  "MAIL_TEMPLATE_AGGREGATE_COMPLETE_PATH"              = "contracts/template/mail/onboarding-complete-aggregate/1.0.0.json",
  "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json",
  "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json",
  "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json",
  "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/onboarding-request/1.0.3.json",
  "MAIL_TEMPLATE_REGISTRATION_AGGREGATOR_PATH"         = "contracts/template/mail/onboarding-request-aggregator/1.0.2.json"
  "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_USER_PATH"               = "contracts/template/mail/onboarding-request-admin/1.0.1.json",
  "MAIL_TEMPLATE_USER_COMPLETE_NOTIFICATION_PATH"      = "contracts/template/mail/onboarding-complete-user/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_USER_NEW_MANAGER_PATH"   = "contracts/template/mail/onboarding-request-manager/1.0.1.json",
  "SELFCARE_ADMIN_NOTIFICATION_URL"                    = "https://selfcare.pagopa.it/dashboard/admin/onboarding/",
  "SELFCARE_URL"                                       = "https://selfcare.pagopa.it",
  "MAIL_ONBOARDING_CONFIRMATION_LINK"                  = "https://selfcare.pagopa.it/onboarding/confirm?jwt=",
  "MAIL_USER_CONFIRMATION_LINK"                        = "https://selfcare.pagopa.it/onboarding/confirm?add-user=true&jwt=",
  "MAIL_ONBOARDING_REJECTION_LINK"                     = "https://selfcare.pagopa.it/onboarding/cancel?jwt=",
  "MAIL_ONBOARDING_URL"                                = "https://selfcare.pagopa.it/onboarding/",
  "MS_CORE_URL"                                        = "https://selc-p-ms-core-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io",
  "JWT_BEARER_TOKEN"                                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)",
  "MS_USER_URL"                                        = "https://selc-p-user-ms-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io",
  "MS_PARTY_REGISTRY_URL"                              = "https://selc-p-party-reg-proxy-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io",
  "USER_MS_SEND_MAIL"                                  = "true",
  "EVENT_HUB_BASE_PATH"                                = "https://selc-p-eventhub-ns.servicebus.windows.net",
  "STANDARD_SHARED_ACCESS_KEY_NAME"                    = "selfcare-wo"
  "EVENTHUB_SC_CONTRACTS_SELFCARE_WO_KEY_LC"           = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/eventhub-sc-contracts-selfcare-wo-key-lc/)"
  "STANDARD_TOPIC_NAME"                                = "SC-Contracts"
  "SAP_SHARED_ACCESS_KEY_NAME"                         = "external-interceptor-wo"
  "EVENTHUB_SC_CONTRACTS_SAP_SELFCARE_WO_KEY_LC"       = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/eventhub-sc-contracts-sap-external-interceptor-wo-key-lc/)"
  "SAP_TOPIC_NAME"                                     = "SC-Contracts-SAP"
  "FD_SHARED_ACCESS_KEY_NAME"                          = "external-interceptor-wo"
  "EVENTHUB_SC_CONTRACTS_FD_SELFCARE_WO_KEY_LC"        = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/eventhub-selfcare-fd-external-interceptor-wo-key-lc/)"
  "FD_TOPIC_NAME"                                      = "Selfcare-FD"
  "SAP_ALLOWED_INSTITUTION_TYPE"                       = "PA,GSP,SA,AS,SCP"
  "SAP_ALLOWED_ORIGINS"                                = "IPA,SELC,PDND_INFOCAMERE"
  "MINUTES_THRESHOLD_FOR_UPDATE_NOTIFICATION"          = "5"
  "BYPASS_CHECK_ORGANIZATION"                          = "false"
  "PROD_FD_URL"                                        = "https://portale.fideiussionidigitali.it"
  "FD_TOKEN_GRANT_TYPE"                                = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/prod-fd-grant-type/)"
  "FD_TOKEN_CLIENT_ID"                                 = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/prod-fd-client-id/)"
  "FD_TOKEN_CLIENT_SECRET"                             = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/prod-fd-client-secret/)"


  ##ARUBA SIGNATURE
  "PAGOPA_SIGNATURE_SOURCE"                        = "namirial",
  "ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH"      = "faPagoPa",
  "ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD"            = "dsign",
  "ARUBA_SIGN_SERVICE_IDENTITY_USER"               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-user/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER"     = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-delegated-user/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD" = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-delegated-psw/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN"   = "faPagoPa",
  "ARUBA_SIGN_SERVICE_BASE_URL"                    = "https://asbr-pagopa.arubapec.it/ArubaSignService/ArubaSignService"
  "ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS"          = "60000"
  "ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS"          = "60000"
  "EMAIL_SERVICE_AVAILABLE"                        = "true"
  "JWT_TOKEN_ISSUER"                               = "SPID"
  "JWT_TOKEN_PRIVATE_KEY"                          = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/jwt-private-key/)"
  "JWT_TOKEN_KID"                                  = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/jwt-kid/)"

  ##WEBHOOK
  "WEBHOOK_BASE_PATH"                              = "https://selc-p-webhook-ms-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io"

  ##NAMIRIAL SIGNATURE
  "NAMIRIAL_BASE_URL"                       = "https://selc-p-namirial-sws-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io",
  "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"     = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/namirial-sign-service-user/)",
  "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD" = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/namirial-sign-service-psw/)",

  ##ENCRYPTION PUB Key
  "ONBOARDING_DATA_ENCRIPTION_KEY" = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/onboarding-data-encryption-key/)",
  "ONBOARDING_DATA_ENCRIPTION_IV"  = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/onboarding-data-encryption-iv/)"

}