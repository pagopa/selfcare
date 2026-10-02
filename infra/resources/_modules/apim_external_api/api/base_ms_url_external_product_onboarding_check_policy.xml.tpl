<policies>
    <inbound>
        <base/>

        <set-header name="Authorization" exists-action="override">
            <value>@((string)context.Variables["jwt"])</value>
        </set-header>

        <set-query-parameter name="productId" exists-action="override">
            <value>@((string)context.Variables["productId"])</value>
        </set-query-parameter>

        <send-request mode="new" response-variable-name="onboardingLookupResponse" timeout="20" ignore-error="true">
            <set-url>@{
                var onboardingId = context.Request.MatchedParameters.GetValueOrDefault("onboardingId", "");
                return "${MS_BACKEND_URL}/onboarding/" + onboardingId;
            }</set-url>
            <set-method>GET</set-method>
            <set-header name="Authorization" exists-action="override">
                <value>@((string)context.Variables["jwt"])</value>
            </set-header>
            <set-header name="X-Tenant-Id" exists-action="override">
                <value>${APP_TENANT_ID}</value>
            </set-header>
        </send-request>

        <choose>
            <when condition="@{
                var response = (IResponse)context.Variables[&quot;onboardingLookupResponse&quot;];
                return response == null;
            }">
                <return-response>
                    <set-status code="502" reason="Bad Gateway"/>
                    <set-header name="Content-Type" exists-action="override">
                        <value>application/problem+json</value>
                    </set-header>
                    <set-body>{"title":"Unable to retrieve onboarding","detail":"onboarding-ms did not return a response (connection error or timeout)."}</set-body>
                </return-response>
            </when>
            <when condition="@{
                var response = (IResponse)context.Variables[&quot;onboardingLookupResponse&quot;];
                return response.StatusCode != 200;
            }">
                <return-response response-variable-name="onboardingLookupResponse"/>
            </when>
        </choose>

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
