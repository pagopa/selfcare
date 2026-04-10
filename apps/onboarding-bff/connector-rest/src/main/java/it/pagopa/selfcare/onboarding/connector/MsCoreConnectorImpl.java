package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.MsCoreConnector;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsCoreRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
class MsCoreConnectorImpl implements MsCoreConnector {

    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution external id is required";
    protected static final String REQUIRED_DESCRIPTION_MESSAGE = "An Institution decription is required";
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    private final MsCoreRestClient restClient;
    public MsCoreConnectorImpl(@RestClient MsCoreRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Institution getInstitutionByExternalId(String externalInstitutionId) {
        log.trace("getInstitution start");
        log.debug("getInstitution externalInstitutionId = {}", externalInstitutionId);
        requireHasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        Institution result = restClient.getInstitutionByExternalId(externalInstitutionId);
        log.debug("getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @Override
    public Institution createInstitutionUsingInstitutionData(CreateInstitutionData createInstitutionData) {
        log.trace("createInstitutionUsingInstitutionData start");
        log.debug("createInstitutionUsingInstitutionData externalId = {}, description = {}", createInstitutionData.getTaxId(), createInstitutionData.getDescription());
        requireHasText(createInstitutionData.getTaxId(), REQUIRED_INSTITUTION_ID_MESSAGE);
        requireHasText(createInstitutionData.getDescription(), REQUIRED_DESCRIPTION_MESSAGE);
        Institution result = restClient.createInstitutionUsingInstitutionData(createInstitutionData);
        log.debug("createInstitutionUsingInstitutionData result = {}", result);
        log.trace("createInstitutionUsingInstitutionData end");
        return result;
    }

    @Override
    public void verifyOnboarding(String externalInstitutionId, String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}, productId = {}", externalInstitutionId, productId);
        requireHasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        requireHasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient.verifyOnboarding(externalInstitutionId, productId);
        log.trace("verifyOnboarding end");
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
