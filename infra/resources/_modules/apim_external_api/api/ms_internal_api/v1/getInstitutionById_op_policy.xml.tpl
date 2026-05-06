<policies>
  <inbound>
    <base/>
       <set-header name="x-selfcare-uid" exists-action="override">
         <value>@(context.Request.Headers.GetValueOrDefault("x-selfcare-uid", ""))</value>
       </set-header>
    <choose>
       <when condition="@{
           var id = context.Request.MatchedParameters["id"] as string;
           return id != null && id.Length == 11 && System.Text.RegularExpressions.Regex.IsMatch(id, "^\\d{11}$");
         }">
           <set-backend-service base-url="${MS_REGISTRY_PROXY_BACKEND_URL}" />
       </when>
       <otherwise>
           <set-backend-service base-url="${MS_BACKEND_URL}" />
       </otherwise>
    </choose>
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
