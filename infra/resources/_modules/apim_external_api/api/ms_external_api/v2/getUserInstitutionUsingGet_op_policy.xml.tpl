<policies>
  <inbound>
    <base/>
    <set-header name="Authorization" exists-action="override">
    <value>@((string)context.Variables["jwt"])</value>
    </set-header>

    <set-query-parameter name="products" exists-action="override">
          <value>@((string)context.Variables["productId"])</value>
     </set-query-parameter>

    <set-backend-service base-url="${MS_BACKEND_URL}" />
  </inbound>
  <backend>
    <base/>
  </backend>
  <outbound>
    <base/>
  </outbound>
  <on-error>
    <base/>
  </on-error>
</policies>