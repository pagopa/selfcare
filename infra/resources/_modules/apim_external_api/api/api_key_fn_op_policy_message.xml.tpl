<policies>
    <inbound>
        <base/>
        
        <set-variable name="fnkey" value="${FN_KEY}"/>
        <set-header name="X-FUNCTIONS-KEY" exists-action="override">
            <value>@((string)context.Variables["fnkey"])</value>
        </set-header>
        <set-backend-service base-url="${BACKEND_BASE_URL}"/>
        <rewrite-uri template="@("/api/acknowledgment/conservazione/message/{messageId}/status/{status}")" />
    </inbound>
    <backend>
    <base/>
    </backend>
    <on-error>
    <base/>
    </on-error>
</policies>
