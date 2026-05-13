<policies>
    <inbound>
        <choose>
            <when condition="@(((string)context.Product.Id).Contains("io-sign"))">
                <set-backend-service base-url="${MS_EXTERNAL_BACKEND_URL}" />
            </when>
            <otherwise>
                <set-backend-service base-url="${MS_BACKEND_URL}" />
            </otherwise>
        </choose>
        <base />
        <set-header name="Authorization" exists-action="override">
            <value>@((string)context.Variables["jwt"])</value>
        </set-header>
        <set-query-parameter name="productId" exists-action="override">
            <value>@((string)context.Variables["productId"])</value>
        </set-query-parameter>
    </inbound>
    <backend>
        <base />
    </backend>
    <outbound>
        <base />
        <choose>
            <when condition="@(context.Response.StatusCode == 200 && ((string)context.Product.Id).Contains("io-sign"))">
                <set-body>@{
                    JArray response = context.Response.Body.As<JArray>();
                    foreach(JObject item in response.Children()) {

                        item["logo"] = new JValue(new Uri("${WEB_STORAGE_URL}/institutions/" + item.GetValue("id") + "/logo.png"));

                        var assistance = item["assistanceContacts"] as JObject;
                        if (assistance != null && assistance["supportEmail"] != null) {
                            item["supportEmail"] = assistance["supportEmail"];
                        }

                        item.Remove("assistanceContacts");
                        }
                    return response.ToString();
                }</set-body>
            </when>
        </choose>
    </outbound>
    <on-error>
        <base />
    </on-error>
</policies>