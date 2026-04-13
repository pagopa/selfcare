###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "uat"
  env_short = "u"
  domain    = "ar"

  dns_zone_prefix                = "uat.selfcare"
  api_dns_zone_prefix            = "api.uat.selfcare"
  private_dns_name_domain        = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-cae-002"
  ca_resource_group_name         = "selc-u-container-app-002-rg"
}

###############################################################################
# ONBOARDING FUNCTIONS
###############################################################################

locals {
  onboarding_functions = {
    name                      = "selc-u-onboarding-fn"
    subnet_cidr               = ["10.1.144.0/24"]
    always_on                 = true
    service_plan_sku          = "B2"
    service_plan_worker_count = 1
    nat_resource_group_name   = "selc-u-nat-rg"
    nat_gateway_name          = "selc-u-nat_gw"
    app_settings = {
      "APPLICATIONINSIGHTS_CONNECTION_STRING"              = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/appinsights-connection-string/)"
      "USER_REGISTRY_URL"                                  = "https://api.uat.pdv.pagopa.it/user-registry/v1"
      "MONGODB_CONNECTION_URI"                             = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/mongodb-connection-string/)"
      "USER_REGISTRY_API_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/user-registry-api-key/)"
      "BLOB_STORAGE_CONN_STRING_PRODUCT"                   = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)"
      "STORAGE_CONTAINER_CONTRACT"                         = "sc-u-documents-blob"
      "STORAGE_CONTAINER_PRODUCT"                          = "selc-u-product"
      "BLOB_STORAGE_CONN_STRING_CONTRACT"                  = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/documents-storage-connection-string/)"
      "MAIL_DESTINATION_TEST_ADDRESS"                      = "pectest@pec.pagopa.it"
      "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json"
      "MAIL_SENDER_ADDRESS"                                = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_USERNAME"                               = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_PASSWORD"                               = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/smtp-psw/)"
      "MAIL_SERVER_HOST"                                   = "smtps.pec.aruba.it"
      "MAIL_SERVER_PORT"                                   = "465"
      "MAIL_SERVER_SSL"                                    = "true"
      "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json"
      "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json"
      "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/portal-admin-operator-email/)"
      "MAIL_TEMPLATE_COMPLETE_PATH"                        = "contracts/template/mail/onboarding-complete/1.0.0.json"
      "MAIL_TEMPLATE_AGGREGATE_COMPLETE_PATH"              = "contracts/template/mail/onboarding-complete-aggregate/1.0.0.json"
      "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json"
      "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json"
      "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/onboarding-request/1.0.1.json"
      "MAIL_TEMPLATE_REGISTRATION_AGGREGATOR_PATH"         = "contracts/template/mail/onboarding-request-aggregator/1.0.1.json"
      "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.0.json"
      "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_USER_PATH"               = "contracts/template/mail/onboarding-request-admin/1.0.0.json"
      "MAIL_TEMPLATE_USER_COMPLETE_NOTIFICATION_PATH"      = "contracts/template/mail/onboarding-complete-user/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_USER_NEW_MANAGER_PATH"   = "contracts/template/mail/onboarding-request-manager/1.0.0.json"
      "SELFCARE_ADMIN_NOTIFICATION_URL"                    = "https://imprese.uat.notifichedigitali.it/dashboard/admin/onboarding/"
      "SELFCARE_URL"                                       = "https://imprese.uat.notifichedigitali.it"
      "MAIL_ONBOARDING_CONFIRMATION_LINK"                  = "https://imprese.uat.notifichedigitali.it/onboarding/confirm?jwt="
      "MAIL_USER_CONFIRMATION_LINK"                        = "https://imprese.uat.notifichedigitali.it/onboarding/confirm?add-user=true&jwt="
      "MAIL_ONBOARDING_REJECTION_LINK"                     = "https://imprese.uat.notifichedigitali.it/onboarding/cancel?jwt="
      "MAIL_ONBOARDING_URL"                                = "https://imprese.uat.notifichedigitali.it/onboarding/"
      "MS_USER_URL"                                        = "https://selc-u-user-ms-ca.foggyforest-4b815f7f.westeurope.azurecontainerapps.io"
      "MS_CORE_URL"                                        = "https://selc-u-ms-core-ca.foggyforest-4b815f7f.westeurope.azurecontainerapps.io"
      "JWT_BEARER_TOKEN"                                   = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)"
      "MS_PARTY_REGISTRY_URL"                              = "https://selc-u-party-reg-proxy-ca.foggyforest-4b815f7f.westeurope.azurecontainerapps.io"
      "USER_MS_SEND_MAIL"                                  = "false"
      "FORCE_INSTITUTION_PERSIST"                          = "true"
      "JWT_TOKEN_ISSUER"                                   = "SPID"
      "JWT_TOKEN_PRIVATE_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/jwt-private-key/)"
      "JWT_TOKEN_KID"                                      = "@Microsoft.KeyVault(SecretUri=https://selc-u-kv.vault.azure.net/secrets/jwt-kid/)"
    }
  }
}
module "onboarding_functions" {
  source = "../../_modules/functions"

  functions_name            = local.onboarding_functions.name
  subnet_cidr               = local.onboarding_functions.subnet_cidr
  always_on                 = local.onboarding_functions.always_on
  service_plan_sku          = local.onboarding_functions.service_plan_sku
  service_plan_worker_count = local.onboarding_functions.service_plan_worker_count
  nat_resource_group_name   = local.onboarding_functions.nat_resource_group_name
  nat_gateway_name          = local.onboarding_functions.nat_gateway_name
  vnet_resource_group_name  = module.local.vnet_resource_group_name
  vnet_name                 = module.local.vnet_selc_name
  key_vault_id              = module.local.key_vault_id
  tenant_id                 = module.local.tenant_id
  replication_type          = "LRS"
  app_settings              = local.onboarding_functions.app_settings
  location                  = module.local.config.location
  tags                      = module.local.config.tags
}