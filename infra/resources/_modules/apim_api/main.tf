resource "azurerm_api_management_api_version_set" "apim_api_version_set" {
  name                = var.api_name
  resource_group_name = var.apim_rg
  api_management_name = var.apim_name
  display_name        = var.display_name
  versioning_scheme   = "Segment"
}


module "apim_api" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v7.26.5"
  name                = var.api_name
  api_management_name = var.apim_name
  resource_group_name = var.apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_api_version_set.id

  description  = var.display_name
  display_name = var.display_name
  path         = var.base_path
  protocols = [
    "https"
  ]

  service_url = "https://${var.private_dns_name}"

  content_format = "openapi+json"
  content_value = templatefile(var.openapi_path, {
    url      = format("%s.%s", var.api_dns_zone_prefix, var.external_domain)
    basePath = var.base_path
  })

  subscription_required = false

  api_operation_policies = [{
    operation_id = "loginSaml"
    xml_content  = <<XML
      <policies>
          <inbound>
              <cors allow-credentials="true">
                  <allowed-origins>
                      <origin>https://${var.dns_zone_prefix}.${var.external_domain}</origin>
                      <origin>https://${var.api_dns_zone_prefix}.${var.external_domain}</origin>
                      <origin>http://localhost:3000</origin>
                      <origin>https://accounts.google.com</origin>
                  </allowed-origins>
                  <allowed-methods>
                      <method>POST</method>
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
  ]

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
