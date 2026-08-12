resource "azurerm_api_management_api_version_set" "apim_api_version_set" {
  name                = var.api_name
  resource_group_name = var.apim_rg
  api_management_name = var.apim_name
  display_name        = var.display_name
  versioning_scheme   = "Segment"
}


locals {
  # Empty string means "no explicit default tenant" -> requests without an Origin/Referer are
  # rejected (fail-closed). See var.default_tenant_id.
  default_tenant_id = var.default_tenant_id == null ? "" : var.default_tenant_id
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
%{for t in var.tenant_ids}
                <origin>${t.origin}</origin>
%{endfor}
%{for o in var.local_development_origins}
                <origin>${o}</origin>
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
            ALWAYS derived here from the calling origin against that origin -> tenant list, never
            trusted from the caller.

            Matching is EXACT on the serialised origin (scheme + authority), never a prefix: a
            prefix test would let https://selfcare.pagopa.it.attacker.example resolve as the AR
            tenant. When the request carries no Origin, the Referer is parsed and reduced to its
            scheme + authority before the same exact lookup.

            A subscription listed in var.service_caller_tenants is resolved FIRST, regardless of
            Origin/Referer. A non-browser caller controls those headers and must not be able to
            override the tenant pinned to its APIM credential by pretending to be a browser.
            Note the caller cannot express its tenant any other way:
            the policy below OVERRIDES X-Tenant-Id unconditionally, so a header set by the calling
            application is discarded — mapping the subscription is the only supported mechanism.

            An unknown subscription then follows browser origin resolution. var.default_tenant_id
            applies to origin-less requests, or to the exact operations listed in
            var.default_tenant_operation_ids (for example a SAML ACS posted from the IdP origin).
            It defaults to null => rejected.
        -->
        <set-variable name="callerOrigin" value="@{
            var origin = context.Request.Headers.GetValueOrDefault("Origin", "");
            if (string.IsNullOrEmpty(origin)) {
                var referer = context.Request.Headers.GetValueOrDefault("Referer", "");
                if (!string.IsNullOrEmpty(referer)) {
                    try {
                        var refererUri = new Uri(referer);
                        origin = refererUri.Scheme + "://" + refererUri.Authority;
                    } catch (Exception) {
                        origin = "";
                    }
                }
            }
            return origin.Trim().ToLowerInvariant();
        }" />
        <set-variable name="resolvedTenant" value="@{
            var tenantByOrigin = new Dictionary<string, string> {
%{for t in var.tenant_ids}
                { "${lower(t.origin)}", "${t.id}" },
%{endfor}
%{for o in var.local_development_origins}
                { "${lower(o)}", "${var.tenant_ids[0].id}" },
%{endfor}
            };
            var tenantBySubscription = new Dictionary<string, string> {
%{for sub, tenant in var.service_caller_tenants}
                { "${lower(sub)}", "${tenant}" },
%{endfor}
            };
            var subscriptionId = context.Subscription == null ? "" : (context.Subscription.Id ?? "").ToLowerInvariant();
            string serviceTenant;
            if (!string.IsNullOrEmpty(subscriptionId) && tenantBySubscription.TryGetValue(subscriptionId, out serviceTenant)) {
                return serviceTenant;
            }

            var defaultOperations = new HashSet<string> {
%{for operation_id in var.default_tenant_operation_ids}
                "${operation_id}",
%{endfor}
            };
            var caller = (string)context.Variables["callerOrigin"];
            if (string.IsNullOrEmpty(caller)) {
                return defaultOperations.Count == 0 || defaultOperations.Contains(context.Operation.Id)
                    ? "${local.default_tenant_id}"
                    : "";
            }
            string tenant;
            if (tenantByOrigin.TryGetValue(caller, out tenant)) {
                return tenant;
            }
            return defaultOperations.Contains(context.Operation.Id) ? "${local.default_tenant_id}" : "";
        }" />
        <choose>
            <when condition="@(string.IsNullOrEmpty((string)context.Variables["resolvedTenant"]))">
                <return-response>
                    <set-status code="403" reason="Forbidden" />
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>@("{\"status\":403,\"title\":\"tenant_url_mismatch\",\"detail\":\"Calling URL does not match any known tenant frontend for this API, or the request carried no Origin/Referer and this API defines no default tenant.\"}")</set-body>
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
