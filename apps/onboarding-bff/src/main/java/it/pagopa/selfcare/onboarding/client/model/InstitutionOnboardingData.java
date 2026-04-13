package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionOnboardingData {

    private InstitutionInfo institution;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private CompanyInformations companyInformations;
    private AssistanceContacts assistanceContacts;
}
