package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Objects;


@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String USER_ID_HEADER = "user-uuid";
    private static final String JWT_BEARER_TOKEN_ENV = "JWT_BEARER_TOKEN";

    @Inject
    JwtSessionService tokenService;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String bearerToken;
        // If user is founded on PDV, a bearer token is created starting from it
        if (!clientOutgoingHeaders.isEmpty() && clientOutgoingHeaders.containsKey(USER_ID_HEADER)) {
            final String uuid = clientOutgoingHeaders.get(USER_ID_HEADER).get(0);
            final String jwt = tokenService.createJwt(uuid);
            bearerToken = Objects.nonNull(jwt) ? jwt : System.getenv(JWT_BEARER_TOKEN_ENV);
        } else {
            bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);
        }
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        return clientOutgoingHeaders;
    }
}