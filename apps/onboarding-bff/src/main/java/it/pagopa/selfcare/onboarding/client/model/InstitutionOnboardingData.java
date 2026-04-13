package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.client.model.AssistanceContacts;
import it.pagopa.selfcare.onboarding.client.model.CompanyInformations;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingData {

    private InstitutionInfo institution;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
