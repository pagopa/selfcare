package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.client.model.institutions.AssistanceContacts;
import it.pagopa.selfcare.onboarding.client.model.institutions.CompanyInformations;
import it.pagopa.selfcare.onboarding.client.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.onboarding.GeographicTaxonomy;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingData {

    private InstitutionInfo institution;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
