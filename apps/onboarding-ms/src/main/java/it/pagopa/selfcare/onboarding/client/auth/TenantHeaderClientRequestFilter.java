package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Forwards the validated tenant to clients that cannot register {@link AuthenticationPropagationHeadersFactory}
 * as {@code ClientHeadersFactory}, because the generator already registers its own one when the spec
 * declares security schemes (onboarding-functions authenticates with the function key).
 * It propagates both the validated tenant and the authenticated bearer token. A configured
 * technical token is used only when the invocation has no HTTP security context.
 */
@ApplicationScoped
public class TenantHeaderClientRequestFilter implements ClientRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String FUNCTION_KEY_HEADER = "x-functions-key";

    private final AuthenticationPropagationHeadersFactory headersFactory;
    private final Instance<JsonWebToken> jsonWebToken;
    private final String technicalToken;
    private final String functionKey;

    @Inject
    public TenantHeaderClientRequestFilter(
            AuthenticationPropagationHeadersFactory headersFactory,
            Instance<JsonWebToken> jsonWebToken,
            @ConfigProperty(name = "onboarding-ms.jwt-bearer-token", defaultValue = "")
            String technicalToken,
            @ConfigProperty(name = "onboarding-ms.onboarding-functions-api-key")
            String functionKey) {
        this.headersFactory = headersFactory;
        this.jsonWebToken = jsonWebToken;
        this.technicalToken = technicalToken;
        this.functionKey = functionKey;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, String> headers =
                headersFactory.update(new MultivaluedHashMap<>(), requestContext.getStringHeaders());
        List<String> tenant = headers.get(TENANT_HEADER);
        if (tenant != null) {
            requestContext.getHeaders().put(TENANT_HEADER, new ArrayList<>(tenant));
        }
        requestContext.getHeaders().putSingle(
                AUTHORIZATION_HEADER, "Bearer " + resolveBearerToken());
        requestContext.getHeaders().putSingle(FUNCTION_KEY_HEADER, functionKey);
    }

    private String resolveBearerToken() {
        try {
            if (jsonWebToken.isResolvable()) {
                String rawToken = jsonWebToken.get().getRawToken();
                if (rawToken != null && !rawToken.isBlank()) {
                    return rawToken;
                }
            }
        } catch (RuntimeException ignored) {
            // There is no active HTTP security context: use the technical token below.
        }
        if (technicalToken == null || technicalToken.isBlank()) {
            throw new IllegalStateException("No bearer token available for onboarding-functions");
        }
        return technicalToken;
    }
}
