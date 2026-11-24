package it.pagopa.selfcare.institution.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "user-ms.tenant-id", defaultValue = "AR")
    String tenantId;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String bearerToken = System.getenv("JWT_BEARER_TOKEN");
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Missing user-ms tenant ID");
        }
        clientOutgoingHeaders.putSingle("X-Tenant-Id", tenantId);
        return clientOutgoingHeaders;
    }
}