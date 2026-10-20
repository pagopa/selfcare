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
            var host = context.Request.Headers.GetValueOrDefault("X-Selfcare-Original-Host", string.Empty);
            if (string.IsNullOrWhiteSpace(host)) {
                return string.Empty;
            }
            host = host.Trim().ToLowerInvariant();
%{for tenant in var.tenant_hosts~}
            if (host == "${lower(tenant.host)}") {
                return "${tenant.id}";
            }
%{endfor~}
            return string.Empty;
        }' />
%{if var.jwt_tenant_resolution_enabled~}
        <set-variable name="jwtTenantId" value='@{
            var authorization = context.Request.Headers.GetValueOrDefault("Authorization", string.Empty);
            if (!authorization.StartsWith("Bearer ")) {
                return string.Empty;
            }
            var jwt = authorization.Substring(7).Trim().AsJwt();
            if (jwt == null || jwt.Audiences == null) {
                return "#INVALID";
            }
            string match = null;
            foreach (var value in jwt.Audiences) {
                var aud = (value ?? string.Empty).Trim().ToLowerInvariant();
                string tenant = null;
%{for tenant in var.tenant_jwt_audiences~}
                if (aud == "${lower(tenant.audience)}") {
                    tenant = "${tenant.id}";
                }
%{endfor~}
                if (tenant == null) {
                    continue;
                }
                if (match != null) {
                    if (match != tenant) {
                        return "#AMBIGUOUS";
                    }
                }
                match = tenant;
            }
            return match ?? "#UNKNOWN";
        }' />
        <choose>
            <when condition='@((string)context.Variables["jwtTenantId"] == "#INVALID")'>
                <trace source="tenant-audit" severity="error">
                    <message>@("event=tenant_request_rejected reason=invalid_jwt operation=" + (context.Operation == null ? "unknown" : context.Operation.Id))</message>
                </trace>
                <return-response>
                    <set-status code="401" reason="Unauthorized" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>{"title":"Unauthorized","status":401,"detail":"Invalid token"}</set-body>
                </return-response>
            </when>
            <when condition='@(((string)context.Variables["jwtTenantId"]).StartsWith("#"))'>
                <trace source="tenant-audit" severity="error">
                    <message>@("event=tenant_request_rejected reason=unknown_audience operation=" + (context.Operation == null ? "unknown" : context.Operation.Id))</message>
                </trace>
                <return-response>
                    <set-status code="403" reason="Forbidden" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>{"title":"Forbidden","status":403,"detail":"Invalid tenant context"}</set-body>
                </return-response>
            </when>
%{for tenant in var.tenant_jwt_audiences~}
            <when condition='@((string)context.Variables["jwtTenantId"] == "${tenant.id}")'>
                <validate-jwt header-name="Authorization" require-scheme="Bearer" failed-validation-httpcode="401" failed-validation-error-message="Unauthorized" require-expiration-time="true" require-signed-tokens="true" output-token-variable-name="tenantJwt">
                    <issuer-signing-keys>
                        <key certificate-id="${tenant.certificate_id}" />
                    </issuer-signing-keys>
                    <audiences>
                        <audience>${tenant.audience}</audience>
                    </audiences>
                    <issuers>
%{for issuer in var.jwt_issuers~}
                        <issuer>${issuer}</issuer>
%{endfor~}
                    </issuers>
                </validate-jwt>
                <set-variable name="tenantId" value='@{
                    var claim = ((Jwt)context.Variables["tenantJwt"]).Claims.GetValueOrDefault("tenant_id", string.Empty);
                    return string.IsNullOrEmpty(claim) || claim == "${tenant.id}" ? "${tenant.id}" : string.Empty;
                }' />
            </when>
%{endfor~}
        </choose>
%{endif~}
        <choose>
            <when condition='@((string)context.Variables["tenantId"] == string.Empty)'>
                <trace source="tenant-audit" severity="error">
                    <message>@("event=tenant_request_rejected reason=" + (context.Variables.ContainsKey("tenantJwt") ? "jwt_tenant_mismatch" : "unknown_host") + " operation=" + (context.Operation == null ? "unknown" : context.Operation.Id))</message>
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
