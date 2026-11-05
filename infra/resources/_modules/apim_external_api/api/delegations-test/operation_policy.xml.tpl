<policies>
  <inbound>
    <choose>
      <when condition='@(context.Subscription == null || context.Product == null || context.Product.Id != "${PRODUCT_ID}")'>
        <return-response>
          <set-status code="403" reason="Forbidden" />
          <set-header name="Content-Type" exists-action="override">
            <value>application/problem+json</value>
          </set-header>
          <set-body>{"title":"Forbidden","status":403,"detail":"A dedicated delegations-test product subscription is required."}</set-body>
        </return-response>
      </when>
    </choose>
    <choose id="delegation-product-check">
      <when condition='@{
        string productId;
        if (context.Request.Method == "GET") {
          productId = context.Request.Url.Query.GetValueOrDefault("productId", "");
        } else if (context.Request.Method == "POST") {
          var body = context.Request.Body?.As&lt;JObject&gt;(preserveContent: true);
          var bodyProductId = body?["productId"];
          if (bodyProductId == null || bodyProductId.Type != JTokenType.String) {
            return true;
          }
          productId = (string)bodyProductId;
        } else {
          return false;
        }
        return !new[] {
          "prod-pn", "prod-pn-dev", "prod-pn-test",
          "prod-pn-svil", "prod-pn-uat", "prod-pn-coll",
          "prod-pn-cert", "prod-pn-hotfix", "prod-pn-prod"
        }.Contains(productId);
      }'>
        <return-response>
          <set-status code="403" reason="Forbidden" />
          <set-header name="Content-Type" exists-action="override">
            <value>application/problem+json</value>
          </set-header>
          <set-body>{"title":"Forbidden","status":403,"detail":"productId must be one of the allowed PN/AR products."}</set-body>
        </return-response>
      </when>
    </choose>
    <set-variable name="tenantId" value="${APP_TENANT_ID}" />
    <base />
  </inbound>
  <backend>
    <base />
  </backend>
  <outbound>
    <base />
  </outbound>
  <on-error>
    <choose>
      <when condition='@(context.Request.Method == "POST" &amp;&amp; context.LastError.PolicyId == "delegation-product-check")'>
        <return-response>
          <set-status code="400" reason="Bad Request" />
          <set-header name="Content-Type" exists-action="override">
            <value>application/problem+json</value>
          </set-header>
          <set-body>{"title":"Bad Request","status":400,"detail":"The request body must be a valid JSON object."}</set-body>
        </return-response>
      </when>
    </choose>
    <base />
  </on-error>
</policies>
