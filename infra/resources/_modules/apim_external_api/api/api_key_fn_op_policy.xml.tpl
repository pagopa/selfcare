<policies>
    <inbound>
         <set-variable name="userid" value="@{
            var uid = "m2m";
            if (context.Subscription != null) {
                uid = (string)context.Subscription.Id;
            }
            return uid;
        }" />
        <set-variable name="username" value="@{
            var name = "apim";
            if (context.Subscription != null) {
                name = (string)context.Subscription.Name;
                if (name == null && context.Product != null) {
                    name = (string)context.Product.Name;
                }
            }
            return name;
        }" />
        <trace source="ONBOARDING_FN" severity="information">
            <message>ONBOARDING User Id</message>
            <metadata name="UserId" value="@((string)context.Variables["userid"])" />
        </trace>
        <trace source="ONBOARDING_FN" severity="information">
            <message>ONBOARDING User Name</message>
            <metadata name="UserName" value="@((string)context.Variables["username"])" />
        </trace>
        <base />
        <set-variable name="fnkey" value="${FN_KEY}" />
        <set-header name="X-FUNCTIONS-KEY" exists-action="override">
            <value>@((string)context.Variables["fnkey"])</value>
        </set-header>
        <set-backend-service base-url="${BACKEND_BASE_URL}" />
    </inbound>
    <backend>
        <forward-request timeout="240" />
    </backend>
    <outbound>
        <base />
    </outbound>
    <on-error>
        <base />
    </on-error>
</policies>
