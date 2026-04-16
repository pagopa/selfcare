locals {
  apim_name = format("selc-%s-apim-v2", var.env_short)
  apim_rg   = format("selc-%s-api-v2-rg", var.env_short)
  api_name  = format("selc-%s-api-bff-proxy", var.env_short)
}


resource "azurerm_api_management_api_version_set" "apim_api_bff_proxy" {
  count               = var.is_pnpg ? 0 : 1
  name                = local.api_name
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
  display_name        = "BFF Proxy API"
  versioning_scheme   = "Segment"
}


module "apim_api_bff_proxy" {
  count               = var.is_pnpg ? 0 : 1
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = local.api_name
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_api_bff_proxy[0].id

  description  = "BFF Proxy API"
  display_name = "BFF Proxy API"
  path         = "party-registry-proxy/v1"
  protocols = [
    "https"
  ]

  service_url = format("https://%s", var.private_dns_name)

  content_format = "openapi+json"
  content_value = templatefile("./apim_api_bff_proxy.json", {
    url      = format("%s.%s", var.api_dns_zone_prefix, var.external_domain)
    basePath = "/party-registry-proxy/v1"
  })

  subscription_required = false

  xml_content = <<XML
<policies>
    <inbound>
        <cors>
            <allowed-origins>
                <origin>https://${var.dns_zone_prefix}.${var.external_domain}</origin>
                <origin>https://${var.api_dns_zone_prefix}.${var.external_domain}</origin>
                <origin>http://localhost:3000</origin>
            </allowed-origins>
            <allowed-methods>
                <method>GET</method>
                <method>POST</method>
                <method>PUT</method>
                <method>DELETE</method>
                <method>HEAD</method>
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
