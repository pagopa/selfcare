package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.model.dto.response.InstitutionOnboardingInfoResource;
import it.pagopa.selfcare.onboarding.model.dto.response.InstitutionResource;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstitutionMapperTest {

    private final InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @Test
    void toOnboardingInstitutionRequest_preservesTheSelectiveProjectionAndCollectionOrder() {
        InstitutionUpdate source = new InstitutionUpdate();
        source.setId("not-forwarded");
        source.setInstitutionType(InstitutionType.PSP);
        source.setAddress("address");
        source.setDescription("description");
        source.setDigitalAddress("pec@example.test");
        source.setTaxCode("tax-code");
        source.setZipCode("00100");
        source.setPaymentServiceProvider(new PaymentServiceProvider());
        source.setDataProtectionOfficer(new DataProtectionOfficer());
        GeographicTaxonomy second = new GeographicTaxonomy();
        second.setCode("second");
        GeographicTaxonomy first = new GeographicTaxonomy();
        first.setCode("first");
        source.setGeographicTaxonomies(List.of(second, first));
        source.setGeographicTaxonomyCodes(List.of("not-forwarded"));
        source.setCity("not-forwarded");
        source.setCounty("not-forwarded");
        source.setCountry("not-forwarded");
        source.setRea("rea");
        source.setShareCapital("1000");
        source.setBusinessRegisterPlace("Roma");
        source.setSupportEmail("support@example.test");
        source.setSupportPhone("123");
        source.setImported(false);
        source.setAdditionalInformations(new AdditionalInformations());
        source.setGpuData(new GPUData());
        source.setLegalForm("not-forwarded");
        source.setOrigin("not-forwarded");
        source.setOriginId("not-forwarded");

        InstitutionLocation location = new InstitutionLocation();
        location.setCity("Roma");
        location.setCounty("RM");
        location.setCountry("IT");
        User firstUser = user("second", PartyRole.MANAGER);
        User secondUser = user("first", PartyRole.DELEGATE);
        OnboardingData data = new OnboardingData();
        data.setInstitutionExternalId("external-id");
        data.setPricingPlan("pricing-plan");
        data.setBilling(new Billing());
        data.setProductId("product-id");
        data.setProductName("product-name");
        data.setInstitutionType(InstitutionType.PA);
        data.setInstitutionUpdate(source);
        data.setLocation(location);
        data.setUsers(List.of(firstUser, secondUser));
        data.setContractPath("contract-path");
        data.setContractVersion("contract-version");

        InstitutionUpdate expected = new InstitutionUpdate();
        expected.setInstitutionType(InstitutionType.PA);
        expected.setAddress("address");
        expected.setDescription("description");
        expected.setDigitalAddress("pec@example.test");
        expected.setTaxCode("tax-code");
        expected.setZipCode("00100");
        expected.setPaymentServiceProvider(source.getPaymentServiceProvider());
        expected.setDataProtectionOfficer(source.getDataProtectionOfficer());
        expected.setCity("Roma");
        expected.setCounty("RM");
        expected.setCountry("IT");
        expected.setGeographicTaxonomyCodes(List.of("second", "first"));
        expected.setRea("rea");
        expected.setShareCapital("1000");
        expected.setBusinessRegisterPlace("Roma");
        expected.setSupportEmail("support@example.test");
        expected.setSupportPhone("123");
        expected.setImported(false);
        expected.setAdditionalInformations(source.getAdditionalInformations());

        OnboardingInstitutionRequest request = institutionMapper.toOnboardingInstitutionRequest(data);

        assertEquals("external-id", request.getInstitutionExternalId());
        assertEquals("pricing-plan", request.getPricingPlan());
        assertSame(data.getBilling(), request.getBilling());
        assertEquals("product-id", request.getProductId());
        assertEquals("product-name", request.getProductName());
        assertEquals(expected, request.getInstitutionUpdate());
        assertNotSame(source, request.getInstitutionUpdate());
        assertSame(source.getPaymentServiceProvider(), request.getInstitutionUpdate().getPaymentServiceProvider());
        assertSame(source.getDataProtectionOfficer(), request.getInstitutionUpdate().getDataProtectionOfficer());
        assertSame(source.getAdditionalInformations(), request.getInstitutionUpdate().getAdditionalInformations());
        assertEquals(List.of(firstUser, secondUser), request.getUsers());
        assertNotSame(firstUser, request.getUsers().get(0));
        assertNotSame(secondUser, request.getUsers().get(1));
        assertEquals("contract-path", request.getContract().getPath());
        assertEquals("contract-version", request.getContract().getVersion());
        assertThrows(UnsupportedOperationException.class, () -> request.getUsers().add(new User()));
        assertThrows(UnsupportedOperationException.class,
                () -> request.getInstitutionUpdate().getGeographicTaxonomyCodes().add("third"));
        assertEquals(List.of("not-forwarded"), source.getGeographicTaxonomyCodes());
        assertEquals("not-forwarded", source.getCity());
    }

    @Test
    void toOnboardingInstitutionRequest_keepsNullsAndAlwaysBuildsTheContract() {
        InstitutionUpdate source = new InstitutionUpdate();
        source.setInstitutionType(InstitutionType.PSP);
        source.setCity("not-forwarded-without-location");
        source.setGeographicTaxonomyCodes(List.of("not-forwarded-without-taxonomies"));
        OnboardingData data = new OnboardingData();
        data.setInstitutionUpdate(source);

        OnboardingInstitutionRequest request = institutionMapper.toOnboardingInstitutionRequest(data);

        assertEquals(new InstitutionUpdate(), request.getInstitutionUpdate());
        assertNull(request.getBilling());
        assertNull(request.getPricingPlan());
        assertEquals(List.of(), request.getUsers());
        assertNotNull(request.getContract());
        assertNull(request.getContract().getPath());
        assertNull(request.getContract().getVersion());

        source.setGeographicTaxonomies(List.of());
        assertEquals(List.of(), institutionMapper.toOnboardingInstitutionRequest(data)
                .getInstitutionUpdate().getGeographicTaxonomyCodes());
    }

    @Test
    void toOnboardingInstitutionRequest_doesNotHideMissingUpdateOrNullCollectionItems() {
        OnboardingData data = new OnboardingData();
        assertThrows(NullPointerException.class, () -> institutionMapper.toOnboardingInstitutionRequest(data));

        data.setInstitutionUpdate(new InstitutionUpdate());
        data.setUsers(Arrays.asList((User) null));
        assertThrows(NullPointerException.class, () -> institutionMapper.toOnboardingInstitutionRequest(data));

        data.setUsers(List.of());
        data.getInstitutionUpdate().setGeographicTaxonomies(Arrays.asList((GeographicTaxonomy) null));
        assertThrows(NullPointerException.class, () -> institutionMapper.toOnboardingInstitutionRequest(data));
    }

    @Test
    void toInstitutionFromIpaPost_preservesFieldsAndNullSubunits() {
        InstitutionFromIpaPost request = institutionMapper.toInstitutionFromIpaPost("tax", "subunit", "AOO");
        assertEquals("tax", request.getTaxCode());
        assertEquals("subunit", request.getSubunitCode());
        assertEquals("AOO", request.getSubunitType());

        request = institutionMapper.toInstitutionFromIpaPost("tax", null, null);
        assertEquals("tax", request.getTaxCode());
        assertNull(request.getSubunitCode());
        assertNull(request.getSubunitType());
    }

    @Test
    void toGetInstitutionRequest_preservesIdOrderIncludingNullIds() {
        InstitutionInfo second = new InstitutionInfo();
        second.setId("second");
        InstitutionInfo first = new InstitutionInfo();
        first.setId("first");

        assertEquals(Arrays.asList("second", null, "first"), institutionMapper.toGetInstitutionRequest(
                List.of(second, new InstitutionInfo(), first)).getInstitutionIds());
        assertEquals(List.of(), institutionMapper.toGetInstitutionRequest(List.of()).getInstitutionIds());
    }

    @Test
    void institutionAssembly_preservesSharedReferencesAndUpdatesOnlyTheLocationProjection() {
        Institution institution = new Institution();
        institution.setGeographicTaxonomies(List.of(new GeographicTaxonomy()));
        institution.setCompanyInformations(new CompanyInformations());
        institution.setAssistanceContacts(new AssistanceContacts());
        institution.setCity("Roma");
        institution.setCounty("RM");
        institution.setCountry("IT");
        institution.setSubunitCode("subunit");
        institution.setSubunitType("AOO");
        institution.setOrigin("IPA");
        InstitutionInfo info = new InstitutionInfo();
        Billing billing = new Billing();
        info.setBilling(billing);
        info.setPricingPlan("existing-plan");
        info.setOriginId("existing-origin-id");

        institutionMapper.updateInstitutionInfo(institution, info);
        InstitutionOnboardingData result = institutionMapper.toInstitutionOnboardingData(institution, info);

        assertSame(info, result.getInstitution());
        assertSame(institution.getGeographicTaxonomies(), result.getGeographicTaxonomies());
        assertSame(institution.getCompanyInformations(), result.getCompanyInformations());
        assertSame(institution.getAssistanceContacts(), result.getAssistanceContacts());
        assertSame(billing, info.getBilling());
        assertEquals("existing-plan", info.getPricingPlan());
        assertEquals("existing-origin-id", info.getOriginId());
        assertEquals("Roma", info.getInstitutionLocation().getCity());
        assertEquals("RM", info.getInstitutionLocation().getCounty());
        assertEquals("IT", info.getInstitutionLocation().getCountry());
        assertEquals("subunit", info.getSubunitCode());
        assertEquals("AOO", info.getSubunitType());
        assertEquals("IPA", info.getOrigin());

        institutionMapper.updateInstitutionInfo(new Institution(), info);
        assertNotNull(info.getInstitutionLocation());
        assertNull(info.getInstitutionLocation().getCity());
        assertNull(info.getInstitutionLocation().getCounty());
        assertNull(info.getInstitutionLocation().getCountry());
        assertNull(info.getSubunitCode());
        assertNull(info.getSubunitType());
        assertNull(info.getOrigin());
    }

    private static User user(String id, PartyRole role) {
        User user = new User();
        user.setId(id);
        user.setName("name-" + id);
        user.setSurname("surname-" + id);
        user.setTaxCode("tax-" + id);
        user.setEmail(id + "@example.test");
        user.setRole(role);
        user.setProductRole("product-role-" + id);
        return user;
    }

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
