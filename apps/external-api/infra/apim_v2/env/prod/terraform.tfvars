# general
env            = "prod"
env_short      = "p"
location       = "westeurope"
location_short = "weu"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

lock_enable = true

# networking
cidr_subnet_apim = ["10.1.162.0/24"]

# dns
dns_zone_prefix    = "selfcare"
dns_zone_prefix_ar = "areariservata"
external_domain    = "pagopa.it"

# apim
apim_publisher_name = "pagoPA SelfCare PROD"
apim_sku            = "Premium_1" # TODO

# aks
private_dns_name                = "selc.internal.selfcare.pagopa.it"
private_onboarding_dns_name     = "selc-p-onboarding-ms-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
ca_suffix_dns_private_name      = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "calmmoss-0be48755.westeurope.azurecontainerapps.io"

# app_gateway
app_gateway_api_certificate_name      = "api-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-selfcare-pagopa-it"