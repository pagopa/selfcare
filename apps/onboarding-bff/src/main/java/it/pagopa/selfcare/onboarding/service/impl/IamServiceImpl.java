package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.IamRestClient;
import it.pagopa.selfcare.onboarding.service.IamService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    public Uni<Boolean> hasIamUserPermission(String permission, String userId, String institutionId, String productId) {
        log.trace("hasIamUserPermission start");
        // Feign omits empty query values; RESTEasy would send an explicit empty parameter.
        return iamRestClient.hasIAMUserPermission(permission, userId,
                        StringUtils.defaultIfEmpty(institutionId, null), StringUtils.defaultIfEmpty(productId, null))
                .map(IamServiceImpl::hasPermission)
                .invoke(() -> log.trace("hasIamUserPermission end"));
    }

    private static boolean hasPermission(Response response) {
        try (response) {
            // Same outcome of the Spring BFF: no body or no flag means no permission
            Map<?, ?> body = response.hasEntity() ? response.readEntity(Map.class) : null;
            return body != null && Boolean.TRUE.equals(body.get(HAS_PERMISSION));
        }
    }
}
