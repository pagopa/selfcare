package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.client.model.AssistanceContacts;
import it.pagopa.selfcare.onboarding.client.model.Billing;
import it.pagopa.selfcare.onboarding.client.model.CompanyInformations;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLocation;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionOnboardingInfoResource;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResource;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstitutionMapperTest {

    private final InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @Test
    void toResource_institutionInfo() {
        InstitutionInfo model = new InstitutionInfo();
        model.setId(UUID.randomUUID().toString());
        model.setDescription("desc");
        model.setTaxCode("tax");
        model.setAddress("addr");
        model.setDigitalAddress("mail");

        InstitutionResource resource = institutionMapper.toResource(model);

        assertNotNull(resource);
        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(model.getDescription(), resource.getDescription());
        assertEquals(model.getTaxCode(), resource.getTaxCode());
        assertEquals(model.getAddress(), resource.getAddress());
        assertEquals(model.getDigitalAddress(), resource.getDigitalAddress());
    }

    @Test
    void toResource_nullInstitutionInfo() {
        assertNull(institutionMapper.toResource((InstitutionInfo) null));
    }

    @Test
    void toResource_institutionOnboardingDataKeepsTheInstitutionIdentity() {
        InstitutionInfo institution = new InstitutionInfo();
        institution.setId("inst-1");
        institution.setInstitutionType(InstitutionType.PA);
        institution.setOrigin("IPA");
        institution.setOriginId("origin-id");
        institution.setDescription("Comune");
        institution.setAddress("Via Roma 1");
        institution.setDigitalAddress("pec@example.it");
        institution.setZipCode("00100");
        institution.setTaxCode("00000000000");
        Billing billing = new Billing();
        billing.setVatNumber("1");
        billing.setRecipientCode("RC1");
        billing.setPublicServices(true);
        institution.setBilling(billing);
        InstitutionLocation location = new InstitutionLocation();
        location.setCity("Roma");
        location.setCounty("RM");
        location.setCountry("IT");
        institution.setInstitutionLocation(location);
        AssistanceContacts contacts = new AssistanceContacts();
        contacts.setSupportEmail("support@example.it");
        CompanyInformations company = new CompanyInformations();
        company.setRea("REA");
        GeographicTaxonomy taxonomy = new GeographicTaxonomy();
        taxonomy.setCode("058091");
        taxonomy.setDesc("Roma");
        InstitutionOnboardingData model = new InstitutionOnboardingData();
        model.setInstitution(institution);
        model.setAssistanceContacts(contacts);
        model.setCompanyInformations(company);
        model.setGeographicTaxonomies(List.of(taxonomy));

        InstitutionOnboardingInfoResource resource = institutionMapper.toResource(model);

        assertEquals("inst-1", resource.getInstitution().getId());
        assertEquals(InstitutionType.PA, resource.getInstitution().getInstitutionType());
        assertEquals("IPA", resource.getInstitution().getOrigin());
        assertEquals("origin-id", resource.getInstitution().getOriginId());
        assertEquals("Roma", resource.getInstitution().getCity());
        assertEquals("RM", resource.getInstitution().getCounty());
        assertEquals("IT", resource.getInstitution().getCountry());
        assertEquals("Comune", resource.getInstitution().getBillingData().getBusinessName());
        assertEquals("Via Roma 1", resource.getInstitution().getBillingData().getRegisteredOffice());
        assertEquals("RC1", resource.getInstitution().getBillingData().getRecipientCode());
        assertEquals("support@example.it", resource.getInstitution().getAssistanceContacts().getSupportEmail());
        assertEquals("REA", resource.getInstitution().getCompanyInformations().getRea());
        assertEquals("058091", resource.getGeographicTaxonomies().get(0).getCode());
    }
}
