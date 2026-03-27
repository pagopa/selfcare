package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.MsCoreConnector;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsCoreRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
class MsCoreConnectorImpl implements MsCoreConnector {

    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution external id is required";
    protected static final String REQUIRED_DESCRIPTION_MESSAGE = "An Institution decription is required";
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    private final MsCoreRestClient restClient;

    @Autowired
    public MsCoreConnectorImpl(MsCoreRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Institution getInstitutionByExternalId(String externalInstitutionId) {
        log.trace("getInstitution start");
        log.debug("getInstitution externalInstitutionId = {}", externalInstitutionId);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        Institution result = restClient.getInstitutionByExternalId(externalInstitutionId);
        log.debug("getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @Override
    public Institution createInstitutionUsingInstitutionData(CreateInstitutionData createInstitutionData) {
        log.trace("createInstitutionUsingInstitutionData start");
        log.debug("createInstitutionUsingInstitutionData externalId = {}, description = {}", createInstitutionData.getTaxId(), createInstitutionData.getDescription());
        Assert.hasText(createInstitutionData.getTaxId(), REQUIRED_INSTITUTION_ID_MESSAGE);
        Assert.hasText(createInstitutionData.getDescription(), REQUIRED_DESCRIPTION_MESSAGE);
        Institution result = restClient.createInstitutionUsingInstitutionData(createInstitutionData);
        log.debug("createInstitutionUsingInstitutionData result = {}", result);
        log.trace("createInstitutionUsingInstitutionData end");
        return result;
    }

    @Override
    public void verifyOnboarding(String externalInstitutionId, String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}, productId = {}", externalInstitutionId, productId);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient.verifyOnboarding(externalInstitutionId, productId);
        log.trace("verifyOnboarding end");
    }

}
