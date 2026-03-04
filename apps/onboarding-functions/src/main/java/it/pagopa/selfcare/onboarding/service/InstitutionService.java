package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

@ApplicationScoped
public class InstitutionService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionService.class);

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    public void deleteByIdAndProductId(String id, String productId) {
        log.debug("Deleting institution {} for product {}", id, productId);
        try (Response response =  institutionApi.deleteOnboardedInstitutionUsingDELETE(productId, id)) {
            if (!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                log.error("Error during institution deletion: {}", response);
                throw new GenericOnboardingException("Impossible to delete institution with ID: " + id);
            }
        }
    }
}
