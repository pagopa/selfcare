locals {
  delegations_test_enabled    = contains(["dev", "uat"], var.env)
  delegations_test_product_id = "delegations-test"
  delegations_test_source     = jsondecode(file("${path.module}/../../../../apps/institution-ms/app/src/main/docs/openapi.json"))
  delegations_test_operations = {
    "/delegations"                = ["get", "post"]
    "/delegations/{delegationId}" = ["delete"]
  }
  delegations_test_openapi = {
    openapi = local.delegations_test_source.openapi
    info = {
      title       = "SelfCare Delegations Test API"
      description = "Retrieve, create and delete SELC delegations in DEV and UAT only."
      version     = "1.0.0"
    }
    servers = [{ url = "https://${local.api_domain}/external/test/v1" }]
    paths = {
      for endpoint, methods in local.delegations_test_operations : endpoint => {
        for method in methods : method => merge(local.delegations_test_source.paths[endpoint][method], {
          tags     = ["Delegations test"]
          security = [{ subscriptionKey = [] }]
          }, method == "get" ? {
          parameters = [
            for parameter in local.delegations_test_source.paths[endpoint][method].parameters :
            merge(parameter, parameter.in == "query" && parameter.name == "productId" ? { required = true } : {})
          ]
        } : {})
      }
    }
    components = {
      schemas = {
        for name in ["DelegationRequest", "DelegationResponse", "Problem", "InvalidParam"] :
        name => local.delegations_test_source.components.schemas[name]
      }
      securitySchemes = {
        subscriptionKey = {
          type = "apiKey"
          name = "Ocp-Apim-Subscription-Key"
          in   = "header"
        }
      }
    }
  }
}

module "apim_product_delegations_test" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_product?ref=v10.9.0"
  count  = local.delegations_test_enabled ? 1 : 0

  product_id   = local.delegations_test_product_id
  display_name = "SelfCare Delegations Test"
  description  = "Dedicated subscriptions for SELC delegation retrieval, creation and deletion in DEV and UAT."

  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  published             = true
  subscription_required = true
  approval_required     = false
}

module "apim_delegations_test_v1" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v10.9.0"
  count  = local.delegations_test_enabled ? 1 : 0

  name                = "${local.project}-delegations-test-api-v1"
  api_management_name = data.azurerm_api_management.apim.name
  resource_group_name = local.rg_apim_name

  description  = local.delegations_test_openapi.info.description
  display_name = local.delegations_test_openapi.info.title
  path         = "external/test/v1"
  protocols    = ["https"]
  service_url  = "https://selc-${var.env_short}-institution-ms-ca.${var.ca_suffix_dns_private_name}/"

  content_format = "openapi+json"
  content_value  = jsonencode(local.delegations_test_openapi)

  xml_content = templatefile("${path.module}/api/jwt_base_policy.xml.tpl", {
    API_DOMAIN                 = local.api_domain
    KID                        = data.azurerm_key_vault_secret.jwt_kid.value
    JWT_CERTIFICATE_THUMBPRINT = azurerm_api_management_certificate.jwt_certificate.thumbprint
    APP_TENANT_ID              = local.tenant_id_ar
  })

  subscription_required = true
  product_ids           = [module.apim_product_delegations_test[0].product_id]

  api_operation_policies = flatten([
    for endpoint, methods in local.delegations_test_operations : [
      for method in methods : {
        operation_id = local.delegations_test_source.paths[endpoint][method].operationId
        xml_content = templatefile("${path.module}/api/delegations-test/operation_policy.xml.tpl", {
          PRODUCT_ID    = local.delegations_test_product_id
          APP_TENANT_ID = local.tenant_id_ar
        })
      }
    ]
  ])
}
