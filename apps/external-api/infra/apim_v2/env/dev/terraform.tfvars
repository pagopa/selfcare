# general
env_short      = "d"
env            = "dev"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

lock_enable = false

# networking
cidr_subnet_apim = ["10.1.161.0/24"]

# dns
dns_zone_prefix    = "dev.selfcare"
dns_zone_prefix_ar = "dev.areariservata"
external_domain    = "pagopa.it"

# apim
apim_publisher_name = "pagoPA SelfCare DEV"
apim_sku            = "Developer_1"

# aks
private_dns_name                = "selc.internal.dev.selfcare.pagopa.it"
private_onboarding_dns_name     = "selc-d-onboarding-ms-ca.gentleflower-c63e62fe.westeurope.azurecontainerapps.io"
ca_suffix_dns_private_name      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "blackhill-644148c0.westeurope.azurecontainerapps.io"

# app_gateway
app_gateway_api_certificate_name      = "api-dev-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-dev-selfcare-pagopa-it"