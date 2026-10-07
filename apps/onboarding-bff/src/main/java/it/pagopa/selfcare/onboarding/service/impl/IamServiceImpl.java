package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.client.IamRestClient;
import it.pagopa.selfcare.onboarding.service.IamService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;

@Slf4j
@ApplicationScoped
public class IamServiceImpl implements IamService {

    private static final String HAS_PERMISSION = "hasPermission";

    private final IamRestClient iamRestClient;

    @Inject
    public IamServiceImpl(@RestClient IamRestClient iamRestClient) {
        this.iamRestClient = iamRestClient;
    }

    @Override
    public boolean hasIamUserPermission(String permission, String userId, String institutionId, String productId) {
        log.trace("hasIamUserPermission start");
        try (Response response = iamRestClient.hasIAMUserPermission(permission, userId, institutionId, productId)) {
            // Same outcome of the Spring BFF: no body or no flag means no permission
            Map<?, ?> body = response.hasEntity() ? response.readEntity(Map.class) : null;
            boolean hasPermission = body != null && Boolean.TRUE.equals(body.get(HAS_PERMISSION));
            log.trace("hasIamUserPermission end");
            return hasPermission;
        }
    }
}
