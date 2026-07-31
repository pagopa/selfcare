package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Objects;


/**
 * Builds the credentials for outgoing calls made from durable-function activities.
 *
 * <p>Downstream services enforce that the {@code X-Tenant-Id} header and the JWT {@code tenant_id}
 * claim are both present and equal, so a token and a header issued independently would be rejected.
 * Both are therefore derived from the same source - the tenant of the onboarding the current
 * activity is processing - which keeps them consistent by construction.
 *
 * <p>The env-provided machine token cannot be given a tenant claim, so when it is used the tenant
 * header is omitted rather than sent on its own: sending a header the token cannot corroborate is
 * exactly the mismatch the downstream filter is meant to catch.
 */
@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String USER_ID_HEADER = "user-uuid";
    private static final String JWT_BEARER_TOKEN_ENV = "JWT_BEARER_TOKEN";

    @Inject
    JwtSessionService tokenService;

    @Inject
    FunctionTenantContext tenantContext;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        final String tenantId = tenantContext.currentTenantId();
        String bearerToken;
        boolean tokenCarriesTenant = false;

        // If user is founded on PDV, a bearer token is created starting from it
        if (!clientOutgoingHeaders.isEmpty() && clientOutgoingHeaders.containsKey(USER_ID_HEADER)) {
            final String uuid = clientOutgoingHeaders.get(USER_ID_HEADER).get(0);
            final String jwt = tokenService.createJwt(uuid, tenantId);
            if (Objects.nonNull(jwt)) {
                bearerToken = jwt;
                tokenCarriesTenant = Objects.nonNull(tenantId);
            } else {
                bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);
            }
        } else {
            bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);
        }
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));

        if (tokenCarriesTenant) {
            clientOutgoingHeaders.put(FunctionTenantContext.TENANT_HEADER, List.of(tenantId));
        }
        return clientOutgoingHeaders;
    }
}
