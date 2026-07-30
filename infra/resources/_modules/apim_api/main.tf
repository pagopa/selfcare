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
                <origin>https://${var.api_dns_zone_prefix}.${var.external_domain}</origin>
                <origin>http://localhost:3000</origin>
%{for t in var.tenant_ids}
                <origin>${t.origin}</origin>
%{endfor}
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
        <!--
            Multitenant tenant resolution & propagation.
            See apps/docs/Multitenant/Step_0/{REQUIREMENTS,ARCHITECTURE,SECURITY}.md (SELC-1, SELC-2).
            Single API group serving every tenant frontend listed in tenant_ids: X-Tenant-Id is
            ALWAYS derived here from the calling Origin/Referer against that origin -> tenant
            list, never trusted from the caller. Origins outside the list are rejected
            (fail-closed); calls without an Origin/Referer (server-to-server, health checks) fall
            back to tenant_ids[0].
        -->
        <set-variable name="callerOrigin" value="@{
            var origin = context.Request.Headers.GetValueOrDefault("Origin", "");
            if (string.IsNullOrEmpty(origin)) {
                origin = context.Request.Headers.GetValueOrDefault("Referer", "");
            }
            return origin;
        }" />
        <set-variable name="resolvedTenant" value="@{
            var tenantByOrigin = new Dictionary<string, string> {
%{for t in var.tenant_ids}
                { "${t.origin}", "${t.id}" },
%{endfor}
                { "http://localhost:3000", "${var.tenant_ids[0].id}" }
            };
            var caller = (string)context.Variables["callerOrigin"];
            if (string.IsNullOrEmpty(caller)) {
                return "${var.tenant_ids[0].id}";
            }
            var match = tenantByOrigin.Keys.FirstOrDefault(o => caller.StartsWith(o));
            return match != null ? tenantByOrigin[match] : "";
        }" />
        <choose>
            <when condition="@(string.IsNullOrEmpty((string)context.Variables["resolvedTenant"]))">
                <return-response>
                    <set-status code="403" reason="Forbidden" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>@("{\"status\":403,\"title\":\"tenant_url_mismatch\",\"detail\":\"Calling URL does not match any known tenant frontend for this API.\"}")</set-body>
                </return-response>
            </when>
        </choose>
        <set-header name="X-Tenant-Id" exists-action="override">
            <value>@((string)context.Variables["resolvedTenant"])</value>
        </set-header>
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
