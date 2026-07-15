#########
## API ##
#########

## monitor ##
module "monitor" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-monitor", var.env_short)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  description  = "Monitor"
  display_name = "Monitor"
  path         = "external/status"
  protocols    = ["https"]

  service_url = null

  content_format = "openapi"
  content_value = templatefile("${path.module}/api/monitor/openapi.json.tpl", {
    host = local.apim_base_url
  })

  xml_content = file("${path.module}/api/base_policy.xml")

  subscription_required = false

  api_operation_policies = [
    {
      operation_id = "get"
      xml_content  = file("${path.module}/api/monitor/mock_policy.xml")
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_external_api_onboarding_auto" {
  name                = format("%s-external-api-onboarding-auto", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "SelfCare Onboarding"
  versioning_scheme   = "Segment"
}

resource "azurerm_api_management_api_version_set" "apim_external_api_onboarding_io" {
  name                = format("%s-external-api-onboarding-io", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "SelfCare Onboarding PA prod-io"
  versioning_scheme   = "Segment"
}

module "apim_external_api_onboarding_auto_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-external-api-onboarding-auto", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_onboarding_auto.id

  description  = "Onboarding API for PA only for io product"
  display_name = "SelfCare Onboarding"
  path         = "external/onboarding-auto"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/external-api-onboarding-auto/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "/onboarding-api/v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true
}

module "apim_external_api_onboarding_io_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-external-api-onboarding-io", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_onboarding_io.id

  description  = "Onboarding API for PA only for io product"
  display_name = "SelfCare Onboarding PA prod-io"
  path         = "external/onboarding-io"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/external-api-onboarding-io/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "/onboarding-api/v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true
}

resource "azurerm_api_management_api_version_set" "apim_external_api_ms" {
  name                = format("%s-ms-external-api", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "External API Service"
  versioning_scheme   = "Segment"
}

module "apim_external_api_ms_v2" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-ms-external-api", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_ms.id

  description  = "This service is the proxy for external services"
  display_name = "External API service"
  path         = "external"
  api_version  = "v2"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/ms_external_api/v2/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v2"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true
  product_ids = [
    module.apim_product_support_io.product_id,
    module.apim_product_interop.product_id,
    module.apim_product_interop_coll.product_id,
    module.apim_product_interop_atst.product_id,
    module.apim_product_pn.product_id,
    module.apim_product_pn_svil.product_id,
    module.apim_product_pn_dev.product_id,
    module.apim_product_pn_coll.product_id,
    module.apim_product_pn_cert.product_id,
    module.apim_product_pn_hotfix.product_id,
    module.apim_product_pn_prod.product_id,
    module.apim_product_pn_test.product_id,
    module.apim_product_pagopa.product_id,
    module.apim_product_idpay.product_id,
    module.apim_product_idpay_merchant.product_id,
    module.apim_product_io_sign.product_id,
    module.apim_product_io.product_id,
    module.apim_product_io_premium.product_id,
    module.apim_product_fd.product_id,
    module.apim_product_fd_garantito.product_id,
    module.apim_product_registro_beni.product_id,
    module.apim_product_idpay_gi.product_id,
    module.apim_product_ced.product_id
  ]

  api_operation_policies = [
    {
      operation_id = "getInstitutionProductsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getUserGroupsUsingGET"
      xml_content = templatefile("${path.module}/api/ms_external_api/v2/jwt_auth_op_policy_user_group.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getUserGroupUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "V2getUserInfoUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "retrieveInstitutionByIdUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getInstitutionUsersByProductUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getContractUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getDelegationsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getDelegationsUsingGET_2"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/v2/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getDelegateInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getDelegatorInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getOnboardingsInstitutionUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/ms_external_api/v2/getInstitutions_op_policy.xml.tpl", {
        MS_BACKEND_URL          = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        MS_EXTERNAL_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
        WEB_STORAGE_URL         = data.azurerm_key_vault_secret.web_storage_url.value
      })
    },
    {
      operation_id = "getUserInfoUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getTokensFromProductUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v1/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "v2getUserInstitution"
      xml_content = templatefile("${path.module}/api/ms_external_api/v2/getUserInstitutionUsingGet_op_policy.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "messageAcknowledgmentUsingPOST"
      xml_content = templatefile("${path.module}/api/api_key_fn_op_policy_message.xml.tpl", {
        BACKEND_BASE_URL       = "https://selc-${var.env_short}-onboarding-fn.azurewebsites.net"
        FN_KEY                 = data.azurerm_key_vault_secret.fn-onboarding-primary-key.value
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
      })
    },
    {
      operation_id = "getOnboardingProduct"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "retrieveInstitutionOnSearchEngine"
      xml_content = templatefile("${path.module}/api/ms_external_api/v2/jwt_base_policy_iss_pagopa.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
        MS_BACKEND_URL             = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "retrieveOnboardingOnSearchEngine"
      xml_content = templatefile("${path.module}/api/ms_external_api/v2/jwt_base_policy_iss_pagopa.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
        MS_BACKEND_URL             = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "createProduct"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_post.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getProductById"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_get.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "patchProductById"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_put.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "uploadContractTemplate"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "downloadContractTemplate"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "listContractTemplates"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
        MS_BACKEND_URL         = "https://selc-${var.env_short}-product-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "createWebhook"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_post.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-webhook-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "updateWebhookbyProductId"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_put.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-webhook-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getWebhookbyProductId"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_policy_get.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-webhook-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "saveIAMUser"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-iam-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getIAMUser"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-iam-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getIAMUserByEmail"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-iam-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getIAMProductRolePermissionsList"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-iam-ms-ca.${var.ca_suffix_dns_private_name}/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "getOnboardingWithFilter"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_policy.xml.tpl", {
        MS_BACKEND_URL         = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
      })
    },
    {
      operation_id = "deleteOnboarding"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_onboarding_check_policy.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "completeOnboardingUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_onboarding_check_policy_size.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "rejectOnboardingUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_product_onboarding_check_policy.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "getContractSigned"
      xml_content = templatefile("${path.module}/api/base_ms_url_external_document_onboarding_check_policy.xml.tpl", {
        MS_ONBOARDING_BE = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
        MS_DOCUMENT_BE   = "https://selc-${var.env_short}-document-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_internal_api_ms" {
  name                = format("%s-ms-internal-api", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "Internal API Service"
  versioning_scheme   = "Segment"
}

module "apim_internal_api_ms_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-ms-internal-api", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_internal_api_ms.id

  description  = "This service is the proxy for internal services"
  display_name = "Internal API service"
  path         = "external/internal"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/ms_internal_api/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "retrieveInstitutionByIdUsingGET"
      xml_content = templatefile("${path.module}/api/ms_internal_api/v1/getInstitutionById_op_policy.xml.tpl", {
        MS_BACKEND_URL                = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
        MS_REGISTRY_PROXY_BACKEND_URL = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "deleteOnboarding"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "onboardingUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getInstitutionUsersByProductUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "createDelegationFromInstitutionsTaxCodeUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "updateUserStatusUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "onboardingInstitutionUsersUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "completeOnboardingTokenConsume"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "completeOnboardingUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "updateCreatedAtUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "onboardingImportUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "institutionPdndByTaxCodeUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "reportContractSigned"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "updateContractSigned"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "onboardingInstitutionUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "onboardingAggregateImport"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      }),
    },
    {
      operation_id = "findUoByUnicodeUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      }),
    },
    {
      operation_id = "findAOOByUnicodeUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      }),
    },
    {
      operation_id = "V2getUserInfoUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getOnboardingProduct"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "getDelegationsUsingGET_2"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "addMembersToUserGroupWithParentInstitutionIdUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "deleteMembersFromUserGroupWithParentInstitutionIdUsingDELETE"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "createUserByUserId"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "uploadContractSigned"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "onboardingAggregationIncrement"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "getUserOtpEmailInfo"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_pdnd_infocamere_api_ms" {
  name                = format("%s-pdnd-infocamere-api", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "PDND Infocamere API Service"
  versioning_scheme   = "Segment"
}


module "apim_pdnd_infocamere_api_ms_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-pdnd-infocamere-api", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_pdnd_infocamere_api_ms.id

  description  = "This service is the proxy for PDND Infocamere services"
  display_name = "PDND Infocamere API service"
  path         = "external/pdnd"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/pdnd_infocamere_api/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "institutionPdndByTaxCodeUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_selfcare_support_service" {
  name                = format("%s-selfcare-support-api-service", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "SelfCare Support API Service"
  versioning_scheme   = "Segment"
}

module "apim_selfcare_support_service_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-selfcare-support-api-service", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_selfcare_support_service.id

  description  = "This service collects the APIs for Support use"
  display_name = "SelfCare Support API service"
  path         = "external/support"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v1/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/selfcare_support_service/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "getContractUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getUserGroupsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "deleteMemberFromUserGroupUsingDELETE"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "deleteMemberFromUserGroupsUsingDELETE"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-group-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "V2getUserInfoUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "getUserInfoUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "updateUserRegistryAndSendNotification"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getInstitutionUsersUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getDelegationsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getDelegationsUsingGET_2"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "createDelegationUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "onboardingInstitutionUsersUsingPOST"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v2/"
      })
    },
    {
      operation_id = "completeOnboardingTokenConsume"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
        }
      )
    },
    {
      operation_id = "onboardingInstitutionUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
        }
      )
    },
    {
      operation_id = "getTokensFromProductUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-ext-api-backend-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "updateOnboardiUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "updateInstitutionUsingPUT"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "sendOnboardigNotificationUsingPOST"
      xml_content = templatefile("${path.module}/api/api_key_fn_op_policy.xml.tpl", {
        BACKEND_BASE_URL = "https://selc-${var.env_short}-onboarding-fn.azurewebsites.net"
        FN_KEY           = data.azurerm_key_vault_secret.fn-onboarding-primary-key.value
      })
    },
    {
      operation_id = "countNotificationsUsingGET"
      xml_content = templatefile("${path.module}/api/api_key_fn_op_policy.xml.tpl", {
        BACKEND_BASE_URL = "https://selc-${var.env_short}-onboarding-fn.azurewebsites.net"
        FN_KEY           = data.azurerm_key_vault_secret.fn-onboarding-primary-key.value
      })
    },
    {
      operation_id = "deleteDelegationUsingDELETE"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "retrieveInstitutionOnSearchEngine"
      xml_content = templatefile("${path.module}/api/jwt_base_policy_iss_pagopa.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
        MS_BACKEND_URL             = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "retrieveOnboardingOnSearchEngine"
      xml_content = templatefile("${path.module}/api/jwt_base_policy_iss_pagopa.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
        MS_BACKEND_URL             = "https://selc-${var.env_short}-party-reg-proxy-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getUserOtpEmailInfo"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-user-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getDelegateInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    },
    {
      operation_id = "getDelegatorInstitutionsUsingGET"
      xml_content = templatefile("${path.module}/api/base_ms_url_policy.xml", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_notification_event_api" {
  name                = format("%s-notification-event-api", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "Notification Event API Service"
  versioning_scheme   = "Segment"
}

module "apim_notification_event_api_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-notification-event-api", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_notification_event_api.id

  description  = "This service is the proxy for internal services"
  display_name = "Notification Event API service"
  path         = "external/notification-event"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/notification_event_api/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/notification_event_api/v1/internal_jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "resendUsersUsingPOST"
      xml_content = templatefile("${path.module}/api/notification_event_api/v1/internal_jwt_base_policy.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
      })
    },
    {
      operation_id = "resendContractsUsingPOST"
      xml_content = templatefile("${path.module}/api/notification_event_api/v1/internal_jwt_base_policy.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
      })
    }
    #{
    #  operation_id = "countUsersUsingGET"
    #  xml_content = templatefile("${path.module}/api/notification_event_api/v1/internal_jwt_base_policy.xml.tpl", {
    #    API_DOMAIN                 = local.api_domain
    #    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    #    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
    #  })
    #},
  ]
}
resource "azurerm_api_management_api_version_set" "apim_external_api_contract" {
  name                = format("%s-external-api-contract", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "External API Contract limited by IP source"
  versioning_scheme   = "Segment"
}

module "apim_external_api_contract_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-external-api-contract-service", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_contract.id

  description  = "This service is the proxy for external get contract limited by IP source"
  display_name = "External API service get contract limited by IP source"
  path         = "external/contract"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v2/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/external_api_contract/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "getContractUsingGET"
      xml_content = templatefile("${path.module}/api/external_api_contract/v1/getContractUsingGet_op_policy.xml.tpl", {
        API_DOMAIN                 = local.api_domain
        KID                        = data.azurerm_key_vault_secret.jwt_kid.value
        JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_external_api_contracts_public" {
  name                = format("%s-external-api-contracts-public", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "External API Contracts Public"
  versioning_scheme   = "Segment"
}

module "apim_external_api_contract_public_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-external-api-contracts-public", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_external_api_contracts_public.id

  description  = "Proxy for external get contract used by conservazione"
  display_name = "External API service get contract"
  path         = "external/contracts"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-ext-api-backend-ca.%s/v2/", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/external_api_contract/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [

    {
      operation_id = "messageAcknowledgmentUsingPOST"
      xml_content = templatefile("${path.module}/api/api_key_fn_op_policy_message.xml.tpl", {
        BACKEND_BASE_URL       = "https://selc-${var.env_short}-onboarding-fn.azurewebsites.net"
        FN_KEY                 = data.azurerm_key_vault_secret.fn-onboarding-primary-key.value
        EXTERNAL-OAUTH2-ISSUER = data.azurerm_key_vault_secret.external-oauth2-issuer.value
        TENANT_ID              = data.azurerm_client_config.current.tenant_id
      })
    }
  ]
}

resource "azurerm_api_management_api_operation" "check_recipient_code" {
  operation_id        = "checkRecipientCode"
  api_name            = module.apim_billing_portal_v1.name
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  display_name = "Check recipient code"
  method       = "GET"
  url_template = "/institutions/onboarding/recipientCode/verification"
  description  = "Check if a recipientCode is valid"

  response {
    status_code = 200
  }

  response {
    status_code = 401
  }

  response {
    status_code = 403
  }
}

resource "azurerm_api_management_api_version_set" "apim_billing_portal" {
  name                = "${var.env_short}-billing-portal"
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "Billing Portal API Service"
  versioning_scheme   = "Segment"
}

module "apim_billing_portal_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = "${local.project}-billing-portal"
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_billing_portal.id

  description  = "This service is the proxy for billing portal services"
  display_name = "Billing Portal API service"
  path         = "external/billing-portal"
  api_version  = "v1"
  protocols = [
    "https"
  ]

  service_url = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/billing-portal-api/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "v1"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

  api_operation_policies = [
    {
      operation_id = "updateOnboardingRecipientIdUsingPUT"
      xml_content = templatefile("${path.module}/api/base_policy_config.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    },
    {
      operation_id = "checkRecipientCode"
      xml_content = templatefile("${path.module}/api/base_policy_billing_portal_config.xml.tpl", {
        MS_BACKEND_URL = "https://selc-${var.env_short}-onboarding-ms-ca.${var.ca_suffix_dns_private_name}/v1/"
      })
    }
  ]
}

resource "azurerm_api_management_api_version_set" "apim_internal_user_api_ms" {
  name                = format("%s-ms-internal-user-api", var.env_short)
  resource_group_name = local.rg_apim_name
  api_management_name = data.azurerm_api_management.apim.name
  display_name        = "Internal User API Service"
  versioning_scheme   = "Segment"
}

module "apim_internal_user_api_ms_v1" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  name                = format("%s-ms-internal-user-api", local.project)
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name
  version_set_id      = azurerm_api_management_api_version_set.apim_internal_user_api_ms.id

  description  = "This service is the proxy for internal User services"
  display_name = "Internal User MS API service"
  path         = "internal/user"
  protocols = [
    "https"
  ]

  service_url = format("https://selc-%s-user-ms-ca.%s", var.env_short, var.ca_suffix_dns_private_name)

  content_format = "openapi+json"
  content_value = templatefile("${path.module}/api/internal_user_api/v1/openapi.${var.env}.json", {
    host     = local.api_domain
    basePath = "internal/user"
  })

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
  })

  subscription_required = true

}

##############
## Products ##
##############

module "apim_product_interop" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "interop"
  display_name = "INTEROP"
  description  = "Interoperabilità"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/interop/policy.xml")
}

module "apim_product_interop_coll" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "interop-coll"
  display_name = "INTEROP COLLAUDO"
  description  = "Interoperabilità Collaudo"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/interop-coll/policy.xml")
}

module "apim_product_interop_atst" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "interop-atst"
  display_name = "INTEROP ATTESTAZIONE"
  description  = "Interoperabilità Attestazione"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/interop-atst/policy.xml")
}

module "apim_product_pn" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn"
  display_name = "PN"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn/policy.xml")
}

module "apim_product_pn_svil" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-svil"
  display_name = "PN SVIL"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_svil/policy.xml")
}

module "apim_product_pn_dev" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-dev"
  display_name = "PN DEV"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_dev/policy.xml")
}

module "apim_product_pn_uat" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-uat"
  display_name = "PN UAT"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_uat/policy.xml")
}

module "apim_product_pn_test" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-test"
  display_name = "PN TEST"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_test/policy.xml")
}

module "apim_product_pn_coll" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-coll"
  display_name = "PN COLL"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_coll/policy.xml")
}

module "apim_product_pn_hotfix" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-hotfix"
  display_name = "PN HOTFIX"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_hotfix/policy.xml")
}

module "apim_product_pn_cert" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-cert"
  display_name = "PN CERT"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_cert/policy.xml")
}

module "apim_product_pn_prod" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pn-prod"
  display_name = "PN PROD"
  description  = "Piattaforma Notifiche"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pn_prod/policy.xml")
}

module "apim_product_pagopa" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "pagopa"
  display_name = "PAGOPA"
  description  = "Pagamenti pagoPA"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/pagopa/policy.xml")
}

module "apim_product_idpay" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "idpay"
  display_name = "IDPAY"
  description  = "ID Pay"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/idpay/policy.xml")
}

module "apim_product_idpay_merchant" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "idpay-merchant"
  display_name = "IDPAY MERCHANT"
  description  = "ID Pay Merchant"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/idpay-merchant/policy.xml")
}

module "apim_product_idpay_gi" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "idpay-gi"
  display_name = "IDPAY GI"
  description  = "ID Pay Gestione iniziative"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/idpay-gi/policy.xml")
}

module "apim_product_io_sign" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "io-sign"
  display_name = "io-sign"
  description  = "Firma con IO"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/io-sign/policy.xml")
}

module "apim_product_io" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "io"
  display_name = "IO"
  description  = "App IO"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/io/policy.xml")
}

module "apim_product_io_premium" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "io-premium"
  display_name = "IO Premium"
  description  = "App IO Premium"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/io-premium/policy.xml")
}

module "apim_product_test_io" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "test-io"
  display_name = "Test IO"
  description  = "Test App IO"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/test-io/policy.xml")
}

module "apim_product_test_io_premium" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "test-io-premium"
  display_name = "Test IO Premium"
  description  = "Test App IO Premium"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/test-io-premium/policy.xml")
}

module "apim_product_support_io" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "prod-io"
  display_name = "Support IO"
  description  = "Support for APP IO"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/support-io/policy.xml")
}

module "apim_product_fd" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "prod-fd"
  display_name = "Fideiussioni Digitali"
  description  = "Fideiussioni Digitali"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/prod-fd/policy.xml")
}
module "apim_product_fd_garantito" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "prod-fd-garantito"
  display_name = "Fideiussioni Digitali Garantito"
  description  = "Fideiussioni Digitali Garantito"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/prod-fd-garantito/policy.xml")
}

module "apim_product_registro_beni" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "prod-registro-beni"
  display_name = "Registro Beni"
  description  = "Registro Beni"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/registro-beni/policy.xml")
}

module "apim_product_ced" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"

  product_id   = "prod-ced"
  display_name = "CED"
  description  = "CED"

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false

  policy_xml = file("${path.module}/api_product/ced/policy.xml")
}

##################
## Named values ##
##################

data "azurerm_key_vault_secret" "apim_backend_access_token" {
  name         = "apim-backend-access-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "external-oauth2-issuer" {
  name         = "external-oauth2-issuer"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "fn-onboarding-primary-key" {
  name         = "fn-onboarding-primary-key"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}
