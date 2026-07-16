package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.client.IamRestClient;
import it.pagopa.selfcare.onboarding.service.IamService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import java.io.IOException;
import jakarta.ws.rs.ProcessingException;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.util.Map;

@Slf4j
@ApplicationScoped
public class IamServiceImpl implements IamService {

    private final IamRestClient iamRestClient;

    @Inject
    public IamServiceImpl(@RestClient IamRestClient iamRestClient) {
        this.iamRestClient = iamRestClient;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    @Override
    public boolean hasIamUserPermission(String permission, String userId, String institutionId, String productId) {
        log.trace("hasIamUserPermission start");
        try (Response response = iamRestClient.hasIAMUserPermission(permission, userId, institutionId, productId)) {
            if (response.getStatus() == 200) {
                Map body = response.readEntity(Map.class);
                boolean hasPermission = Boolean.TRUE.equals(body.get("hasPermission"));
                log.trace("hasIamUserPermission end");
                return hasPermission;
            }
            return false;
        }
    }
}
