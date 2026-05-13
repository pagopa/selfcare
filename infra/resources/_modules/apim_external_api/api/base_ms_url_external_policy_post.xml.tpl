<policies>
    <inbound>
    <base/>

    <set-header name="Authorization" exists-action="override">
        <value>@((string)context.Variables["jwt"])</value>
    </set-header>

    <set-body template="none">@{
        var body = context.Request.Body.As<JObject>(preserveContent: true);
        body["productId"] = (string)context.Variables["productId"];
        return body.ToString();
    }</set-body>

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
