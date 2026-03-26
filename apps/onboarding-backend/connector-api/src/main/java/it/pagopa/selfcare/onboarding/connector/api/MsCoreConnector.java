package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;

public interface MsCoreConnector {

    Institution getInstitutionByExternalId(String externalInstitutionId);

    Institution createInstitutionUsingInstitutionData(CreateInstitutionData createPnPGData);

    void verifyOnboarding(String externalInstitutionId, String productId);

}
