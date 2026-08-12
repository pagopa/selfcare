resource "azurerm_api_management_api_version_set" "apim_api_version_set" {
  name                = var.api_name
  resource_group_name = var.apim_rg
  api_management_name = var.apim_name
  display_name        = var.display_name
  versioning_scheme   = "Segment"
}

locals {
  tenant_origins = {
    for tenant in var.tenant_ids : lower(tenant.origin) => tenant.id
  }

  # The JSON is embedded in an APIM policy XML attribute.
  tenant_origins_policy_json = replace(jsonencode(local.tenant_origins), "\"", "&quot;")
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
                <header>*</header>
            </allowed-headers>
        </cors>
        <set-variable name="tenantId" value="@{
            var caller = context.Request.Headers.GetValueOrDefault(&quot;Origin&quot;, context.Request.Headers.GetValueOrDefault(&quot;Referer&quot;, &quot;&quot;));
            Uri callerUri;
            if (!Uri.TryCreate(caller, UriKind.Absolute, out callerUri)) {
                return string.Empty;
            }

            var origin = callerUri.GetLeftPart(UriPartial.Authority).ToLowerInvariant();
            var tenantOrigins = Newtonsoft.Json.JsonConvert.DeserializeObject<System.Collections.Generic.Dictionary<string, string>>(&quot;${local.tenant_origins_policy_json}&quot;);
            string tenantId;
            return tenantOrigins.TryGetValue(origin, out tenantId) ? tenantId : string.Empty;
        }" />
        <choose>
            <when condition="@((string)context.Variables[&quot;tenantId&quot;] == string.Empty)">
                <return-response>
                    <set-status code="403" reason="Forbidden" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>{"title":"Unknown tenant origin","status":403}</set-body>
                </return-response>
            </when>
        </choose>
        <set-header name="X-Tenant-Id" exists-action="override">
            <value>@((string)context.Variables[&quot;tenantId&quot;])</value>
        </set-header>
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
