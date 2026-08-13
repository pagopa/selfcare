resource "azurerm_api_management_api_version_set" "apim_api_version_set" {
  name                = var.api_name
  resource_group_name = var.apim_rg
  api_management_name = var.apim_name
  display_name        = var.display_name
  versioning_scheme   = "Segment"
}

module "apim_api" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v9.4.0"
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
  content_value = replace(
    templatefile(var.openapi_path, {
      url      = format("%s.%s", var.api_dns_zone_prefix, var.external_domain)
      basePath = var.base_path
    }),
    "/\"title\"\\s*:\\s*\"[^\"]*\"/",
    "\"title\" : \"${var.display_name}\""
  )

  subscription_required = false

  api_operation_policies = var.api_operation_policies

  xml_content = <<XML
<policies>
    <inbound>
        <cors allow-credentials="true">
            <allowed-origins>
%{for tenant in var.tenant_ids~}
                <origin>${tenant.origin}</origin>
%{endfor~}
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
%{for header in var.allowed_headers~}
                <header>${header}</header>
%{endfor~}
            </allowed-headers>
        </cors>
%{if var.tenant_enforcement_enabled~}
        <set-variable name="tenantId" value='@{
            var host = context.Request.OriginalUrl.Host;
            if (string.IsNullOrWhiteSpace(host)) {
                return string.Empty;
            }
            host = host.ToLowerInvariant();
%{for tenant in var.tenant_hosts~}
            if (host == "${lower(tenant.host)}") {
                return "${tenant.id}";
            }
%{endfor~}
            return string.Empty;
        }' />
        <choose>
            <when condition='@((string)context.Variables["tenantId"] == string.Empty)'>
                <trace source="tenant-audit" severity="error">
                    <message>@("event=tenant_request_rejected reason=unknown_host operation=" + (context.Operation == null ? "unknown" : context.Operation.Id))</message>
                </trace>
                <return-response>
                    <set-status code="403" reason="Forbidden" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>{"title":"Forbidden","status":403,"detail":"Invalid tenant context"}</set-body>
                </return-response>
            </when>
        </choose>
        <set-header name="X-Tenant-Id" exists-action="override">
            <value>@((string)context.Variables["tenantId"])</value>
        </set-header>
%{else~}
        <set-header name="X-Tenant-Id" exists-action="delete" />
%{endif~}
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
