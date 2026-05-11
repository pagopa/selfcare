<policies>
    <inbound>
        <choose>
            <when condition="@(((string)context.Product.Id).Contains(" prod-fd"))">
            <validate-jwt header-name="Authorization" failed-validation-httpcode="401"
                          failed-validation-error-message="Unauthorized" require-expiration-time="false"
                          require-scheme="Bearer" require-signed-tokens="true">
                <openid-config url="https://login.microsoftonline.com/${TENANT_ID}/.well-known/openid-configuration"/>
                <required-claims>
                    <claim name="aud" match="all">
                        <value>${EXTERNAL-OAUTH2-ISSUER}</value>
                    </claim>
                </required-claims>
            </validate-jwt>
        </when>
    </choose>
    <base/>

    <set-header name="Authorization" exists-action="override">
        <value>@((string)context.Variables["jwt"])</value>
    </set-header>

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
