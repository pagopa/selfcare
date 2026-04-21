package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String JWT_BEARER_TOKEN_ENV = "JWT_BEARER_TOKEN";

    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        log.trace("AuthenticationPropagationHeadersFactory - incomingHeaders: {}", incomingHeaders.keySet());
        log.trace("AuthenticationPropagationHeadersFactory - clientOutgoingHeaders: {}", clientOutgoingHeaders.keySet());

        final String bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);

        if (Objects.isNull(bearerToken)) {
            log.warn("AuthenticationPropagationHeadersFactory - JWT_BEARER_TOKEN environment variable is not set, skipping Authorization header");
            return clientOutgoingHeaders;
        }

        log.trace("AuthenticationPropagationHeadersFactory - JWT_BEARER_TOKEN is present, length: {}, starts with: {}",
                bearerToken.length(),
                bearerToken.substring(0, Math.min(20, bearerToken.length())));

        final long periodCount = bearerToken.chars().filter(c -> c == '.').count();
        log.trace("AuthenticationPropagationHeadersFactory - JWT period count (expected 2): {}", periodCount);

        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        log.debug("AuthenticationPropagationHeadersFactory - Authorization header set successfully");
        return clientOutgoingHeaders;
    }
}


