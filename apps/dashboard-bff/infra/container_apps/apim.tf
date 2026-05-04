locals {
  apim_name    = format("selc-%s-apim-v2", var.env_short)
  apim_rg      = format("selc-%s-api-v2-rg", var.env_short)
  api_name     = var.is_pnpg ? format("selc-%s-pnpg-api-bff-dashboard", var.env_short) : format("selc-%s-api-bff-dashboard", var.env_short)
  display_name = var.is_pnpg ? "BFF PNPG Dashboard API" : "BFF Dashboard API"
  base_path    = var.is_pnpg ? "imprese/dashboard" : "dashboard"
  openapi_title= var.is_pnpg ? "selc-pnpg-dashboard" : "selc-dashboard"
}


resource "azurerm_api_management_api_version_set" "apim_api_bff_dashboard" {
  name                = local.api_name
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = local.display_name
  versioning_scheme   = "Segment"
}


module "apim_api_bff_dashboard" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = local.api_name
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_api_bff_dashboard.id

  description  = local.display_name
  display_name = local.display_name
  path         = local.base_path
  protocols = [
    "https"
  ]

  service_url = format("https://%s", var.private_dns_name)

  content_format = "openapi+json"
  content_value = templatefile("../../src/main/resources/swagger/api-docs.json", {
    openapi_title = local.openapi_title
    url           = format("%s.%s", var.api_dns_zone_prefix, var.external_domain)
    basePath      = local.base_path
  })

  subscription_required = false

  xml_content = <<XML
<policies>
    <inbound>
        <cors allow-credentials="true">
            <allowed-origins>
                <origin>https://${var.dns_zone_prefix}.${var.external_domain}</origin>
                <origin>https://${var.api_dns_zone_prefix}.${var.external_domain}</origin>
                <origin>http://localhost:3000</origin>
            </allowed-origins>
            <allowed-methods>
                <method>GET</method>
                <method>POST</method>
                <method>PUT</method>
                <method>HEAD</method>
                <method>DELETE</method>
                <method>OPTIONS</method>
            </allowed-methods>
            <allowed-headers>
                <header>*</header>
            </allowed-headers>
        </cors>
        <base />
    </inbound>
    <backend>
        <base />
    </backend>
    <outbound>
        <base />
    </outbound>
    <on-error>
        <base />
    </on-error>
</policies>
XML
}
