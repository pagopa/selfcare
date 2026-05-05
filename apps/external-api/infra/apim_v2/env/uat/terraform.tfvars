# general
env            = "uat"
env_short      = "u"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

lock_enable = true

# networking
cidr_subnet_apim = ["10.1.161.0/24"]

# dns
dns_zone_prefix    = "uat.selfcare"
dns_zone_prefix_ar = "uat.areariservata"
external_domain    = "pagopa.it"

# apim
apim_publisher_name = "pagoPA SelfCare UAT"
apim_sku            = "Developer_1"

# aks
private_dns_name                = "selc.internal.uat.selfcare.pagopa.it"
private_onboarding_dns_name     = "selc-u-onboarding-ms-ca.calmsky-143987c1.westeurope.azurecontainerapps.io"
ca_suffix_dns_private_name      = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"

# app_gateway
app_gateway_api_certificate_name      = "api-uat-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-uat-selfcare-pagopa-it"
