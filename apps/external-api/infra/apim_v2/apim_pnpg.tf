module "apim_pnpg" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"

  product_id   = "pnpg-be"
  display_name = local.apim_pnpg_api.display_name
  description  = local.apim_pnpg_api.display_name

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = false
  subscription_required = false
  approval_required     = false
  # subscriptions_limit   = 1000

  policy_xml = file("./api_pnpg/base_policy.xml")
}

locals {
  apim_pnpg_api = {
    display_name          = "PnPg Product"
    description           = "API to manage PNPG operations"
    path                  = "pnpg"
    subscription_required = false
    service_url           = null
  }
  apim_name       = module.apim.name
  apim_rg         = azurerm_resource_group.rg_api.name
  api_pnpg_domain = format("api-pnpg.%s.%s", var.dns_zone_prefix, var.external_domain)
  pnpg_hostname   = var.env == "prod" ? "api-pnpg.selfcare.pagopa.it" : "api-pnpg.${var.env}.selfcare.pagopa.it"
  project_pnpg    = "${var.prefix}-${var.env_short}-${var.location_short}-pnpg"

  cdn_storage_hostname = "${var.prefix}${var.env_short}${var.location_short}${var.domain}checkoutsa"
}
#########
## API ##
#########

resource "azurerm_api_management_api_version_set" "apim_external_api_data_vault" {
  name                = format("%s-external-api-data-vault", var.env_short)
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = "Data Vault for PNPG"
  versioning_scheme   = "Segment"
}

module "apim_pnpg_external_api_data_vault_v1" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = format("%s-external-api-pnpg", local.project_pnpg)
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_data_vault.id

  description  = "External API Data Vault"
  display_name = "External API Data Vault"
  path         = "external/data-vault"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-pnpg-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_pnpg_suffix_dns_private_name)


  content_format = "openapi+json"
  content_value = templatefile("./api_pnpg/external_api_data_vault/v1/openapi.${var.env}.json", {
    host     = local.pnpg_hostname
    basePath = "v1"
  })

  xml_content = templatefile("./api_pnpg/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_pnpg_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid_pnpg.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate_pnpg.thumbprint
  })

  subscription_required = true
  product_ids = [
    module.apim_pnpg_product_pn_pg.product_id,
    module.apim_product_pnpg_uat_coll.product_id,
    module.apim_product_pnpg_uat_svil.product_id,
    module.apim_product_pnpg_uat.product_id,
    module.apim_product_pnpg_dev.product_id,
    module.apim_product_pnpg_uat_cert.product_id,
    module.apim_product_pnpg_test.product_id
  ]

  api_operation_policies = [
    {
      operation_id = "addInstitutionUsingPOST"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "getInstitution"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-institution-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_external_api_v2_for_pnpg" {
  name                = format("%s-ms-external-api-pnpg", var.env_short)
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = "External API Service for PNPG"
  versioning_scheme   = "Segment"
}

module "apim_pnpg_external_api_ms_v2" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = format("%s-ms-external-api-pnpg", local.project_pnpg)
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_v2_for_pnpg.id

  description  = "This service is the proxy for external services"
  display_name = "External API service for PNPG"
  path         = "external/pn-pg"
  api_version  = "v2"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-pnpg-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_pnpg_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("./api_pnpg/external_api_for_pnpg/v2/openapi.${var.env}.json", {
    host     = local.pnpg_hostname
    basePath = "v1"
  })

  xml_content = templatefile("./api_pnpg/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_pnpg_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid_pnpg.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate_pnpg.thumbprint
  })

  subscription_required = true
  product_ids = [
    module.apim_pnpg_product_pn_pg.product_id,
    module.apim_product_pnpg_uat_coll.product_id,
    module.apim_product_pnpg_uat_svil.product_id,
    module.apim_product_pnpg_uat.product_id,
    module.apim_product_pnpg_dev.product_id,
    module.apim_product_pnpg_uat_cert.product_id,
    module.apim_product_pnpg_test.product_id,
    module.apim_product_pnpg_hotfix.product_id
  ]

  api_operation_policies = [
    {
      operation_id = "getUserGroupsUsingGET"
      xml_content = templatefile("./api/jwt_auth_op_policy_user_group.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-group-ca.${var.ca_pnpg_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "retrieveInstitutionByIdUsingGET"
      xml_content = templatefile("./api/base_ms_url_product_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-institution-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getInstitutionProductsUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getInstitutionUsersByProductUsingGET"
      xml_content = templatefile("./api/base_ms_url_product_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "v2getUserInstitution"
      xml_content = templatefile("./api/base_ms_url_product_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getUserInfoUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "createProduct"
      xml_content = templatefile("./api/base_ms_url_external_policy_post.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getProductById"
      xml_content = templatefile("./api/base_ms_url_external_policy_get.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "patchProductById"
      xml_content = templatefile("./api/base_ms_url_external_policy_put.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_pnpg_support_service" {
  name                = format("%s-support-service-pnpg", var.env_short)
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = "PNPG Support API Service"
  versioning_scheme   = "Segment"
}

module "apim_pnpg_support_service_v2" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = format("%s-support-service-pnpg", local.project_pnpg)
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_pnpg_support_service.id

  description  = "This service collects the APIs for Support use"
  display_name = "PNPG Support API service"
  path         = "external/pn-pg/support"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-pnpg-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_pnpg_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("./api_pnpg/pnpg_support_service/v1/openapi.${var.env}.json", {
    host     = local.pnpg_hostname
    basePath = "v1"
  })

  xml_content = templatefile("./api_pnpg/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_pnpg_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid_pnpg.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate_pnpg.thumbprint
  })


  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "getInstitutionUsersUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getUserGroupsUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-group-ca.${var.ca_pnpg_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "getInstitutionsUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-institution-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "verifyLegalByPOST"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2"
      })
    },
    {
      operation_id = "V2getUserInfoUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2"
      })
    },
    {
      operation_id = "updateUserRegistryAndSendNotification"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "onboardingInstitutionUsersUsingPOST"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-ext-api-backend-ca.${var.ca_pnpg_suffix_dns_private_name}/v2"
      })
    },
    {
      operation_id = "getUserInfoUsingGET"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "institutionsByLegalTaxIdUsingPOST"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-party-reg-proxy-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "legalAddress"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-party-reg-proxy-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "updateInstitutionUsingPUT"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-institution-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_internal_api_for_pnpg" {
  name                = format("%s-internal-api-pnpg", var.env_short)
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = "PNPG Internal API"
  versioning_scheme   = "Segment"
}

module "apim_pnpg_internal_api" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = format("%s-internal-api-pnpg", local.project_pnpg)
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_internal_api_for_pnpg.id

  description  = "This service is the proxy for internal services"
  display_name = "Internal API Service for PNPG"
  path         = "external/internal/pn-pg"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-pnpg-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_pnpg_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("./api_pnpg/internal_api_for_pnpg/v1/openapi.${var.env}.json", {
    host     = local.pnpg_hostname
    basePath = "v1"
  })

  xml_content = templatefile("./api_pnpg/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_pnpg_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid_pnpg.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate_pnpg.thumbprint
  })


  subscription_required = true

  product_ids = [
    module.apim_pnpg_product_pn_pg.product_id,
    module.apim_product_pnpg_uat_coll.product_id,
    module.apim_product_pnpg_uat_svil.product_id,
    module.apim_product_pnpg_uat.product_id,
    module.apim_product_pnpg_dev.product_id,
    module.apim_product_pnpg_uat_cert.product_id,
    module.apim_product_pnpg_test.product_id,
    module.apim_product_pnpg_hotfix.product_id
  ]

  api_operation_policies = [
    {
      operation_id = "createOrUpdateByFiscalCode"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-user-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "onboardingPgCompletion"
      xml_content = templatefile("./api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-pnpg-onboarding-ms-ca.${var.ca_pnpg_suffix_dns_private_name}/v1/"
      })
    }
  ]
}

# PRODUCTS

module "apim_pnpg_product_pn_pg" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg"
  display_name = "PNPG"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg/policy.xml")
}

module "apim_product_pnpg_uat_cert" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-uat-cert"
  display_name = "PNPG UAT CERT"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_uat_cert/policy.xml")
}

module "apim_product_pnpg_uat_coll" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-uat-coll"
  display_name = "PNPG UAT COLL"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_uat_coll/policy.xml")
}

module "apim_product_pnpg_uat_svil" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-uat-svil"
  display_name = "PNPG UAT SVIL"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_uat_svil/policy.xml")
}

module "apim_product_pnpg_uat" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-uat"
  display_name = "PNPG UAT"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_uat/policy.xml")
}

module "apim_product_pnpg_dev" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-dev"
  display_name = "PNPG DEV"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_dev/policy.xml")
}

module "apim_product_pnpg_test" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-test"
  display_name = "PNPG TEST"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_test/policy.xml")
}

module "apim_product_pnpg_hotfix" {
  source       = "git::https://github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v7.26.5"
  product_id   = "prod-pn-pg-hotfix"
  display_name = "PNPG HOTFIX"
  description  = "Piattaforma Notifiche Persone Giuridiche"

  api_management_name = local.apim_name
  resource_group_name = local.apim_rg

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("./api_product_pnpg/pnpg_hotfix/policy.xml")
}

