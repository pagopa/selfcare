<policies>
    <inbound>
    <base/>

    <set-header name="Authorization" exists-action="override">
        <value>@((string)context.Variables["jwt"])</value>
    </set-header>

    <set-variable name="requestpath" value="@{
        string path = context.Operation.UrlTemplate;
        return path.Substring(0, path.LastIndexOf("/"));
    }" />
    <rewrite-uri template="@{
        string basePath = (string)context.Variables["requestpath"];
        string productId = (string)context.Variables["productId"];
        return $"{basePath}/{productId}";
    }" />

    <set-backend-service base-url="${MS_BACKEND_URL}"/>
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
