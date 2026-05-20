<policies>
    <inbound>
        <base/>

        <set-header name="Authorization" exists-action="override">
            <value>@((string)context.Variables["jwt"])</value>
        </set-header>

        <set-query-parameter name="productId" exists-action="override">
            <value>@((string)context.Variables["productId"])</value>
        </set-query-parameter>

        <send-request mode="new" response-variable-name="onboardingLookupResponse" timeout="20" ignore-error="false">
            <set-url>@{
                var onboardingId = context.Request.MatchedParameters.GetValueOrDefault("onboardingId", "");
                return "${MS_ONBOARDING_BE}/onboarding/" + onboardingId;
            }</set-url>
            <set-method>GET</set-method>
            <set-header name="Authorization" exists-action="override">
                <value>@((string)context.Variables["jwt"])</value>
            </set-header>
        </send-request>

        <set-variable name="onboardingProductIdFromLookup" value="@{
            var response = (IResponse)context.Variables[&quot;onboardingLookupResponse&quot;];
            if (response == null || response.StatusCode != 200)
            {
                return string.Empty;
            }

            var body = response.Body.As<string>(preserveContent: true);
            if (string.IsNullOrEmpty(body))
            {
                return string.Empty;
            }

            var json = Newtonsoft.Json.Linq.JObject.Parse(body);
            return (string)json[&quot;productId&quot;] ?? string.Empty;
        }"/>

        <choose>
            <when condition="@{
                var expectedProductId = (string)context.Variables[&quot;productId&quot;];
                var actualProductId = (string)context.Variables[&quot;onboardingProductIdFromLookup&quot;];
                return string.IsNullOrEmpty(expectedProductId)
                       || string.IsNullOrEmpty(actualProductId)
                       || !string.Equals(expectedProductId, actualProductId, System.StringComparison.Ordinal);
            }">
                <return-response>
                    <set-status code="403" reason="Forbidden"/>
                    <set-body>{"title":"Forbidden","detail":"Onboarding does not belong to the subscribed product."}</set-body>
                </return-response>
            </when>
        </choose>

        <set-backend-service base-url="${MS_DOCUMENT_BE}"/>
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
