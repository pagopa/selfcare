package it.pagopa.selfcare.onboarding.connector.model;

import it.pagopa.selfcare.onboarding.connector.model.institutions.AssistanceContacts;
import it.pagopa.selfcare.onboarding.connector.model.institutions.CompanyInformations;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingData {

    private InstitutionInfo institution;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
