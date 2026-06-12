package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.onboarding.*;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.*;

class OnboardingMapperTest {

    private static final String PROD_IO = "prod-io";
    private static final String INSTITUTION_ID = "institutionId";

    @Test
    void fromDtoImportContract() {
        //given
        ImportContractDto model = mockInstance(new ImportContractDto());
        //when
        OnboardingImportContract resource = OnboardingMapperCustom.fromDto(model);
        //then
        assertNotNull(resource);
        assertEquals(model.getOnboardingDate(), resource.getCreatedAt());
        reflectionEqualsByName(model, resource);
    }

    @Test
    void fromDtoImportContract_null() {
        // given
        ImportContractDto importContractDto = null;
        // when
        OnboardingImportContract resource = OnboardingMapperCustom.fromDto(importContractDto);
        // then
        assertNull(resource);
    }

    @Test
    void toOnboardingImportData() {
        //given
        List<UserDto> userDtos = List.of(mockInstance(new UserDto()));
        ImportContractDto importContractDto = mockInstance(new ImportContractDto());
        OnboardingImportDto model = mockInstance(new OnboardingImportDto());
        model.setUsers(userDtos);
        model.setImportContract(importContractDto);
        //when
        OnboardingImportData resource = OnboardingMapperCustom.toOnboardingImportData(INSTITUTION_ID, model);
        //then
        assertNotNull(resource);
        assertEquals(model.getUsers().size(), resource.getUsers().size());
        assertEquals(INSTITUTION_ID, resource.getInstitutionExternalId());
        assertTrue(resource.getInstitutionUpdate().getImported());
        assertEquals(PROD_IO, resource.getProductId());
        assertTrue(resource.getInstitutionUpdate().getImported());
        assertNull(resource.getInstitutionType());
        reflectionEqualsByName(userDtos.get(0), resource.getUsers().get(0));
        reflectionEqualsByName(importContractDto, resource.getContractImported());
    }

    @Test
    void toOnboardingDataWithContract() {
        //given
        List<UserDto> users = List.of(mockInstance(new UserDto()));
        ImportContractDto importContractDto = mockInstance(new ImportContractDto());
        OnboardingImportDto model = mockInstance(new OnboardingImportDto());
        model.setUsers(users);
        model.setImportContract(importContractDto);
        //when
        OnboardingData resource = OnboardingMapperCustom.toOnboardingData(INSTITUTION_ID, model);
        //then
        assertNotNull(resource);
        assertEquals(model.getUsers().size(), resource.getUsers().size());
        assertEquals(INSTITUTION_ID, resource.getInstitutionExternalId());
        assertTrue(resource.getInstitutionUpdate().getImported());
        assertEquals(PROD_IO, resource.getProductId());
        assertEquals(Boolean.TRUE, resource.getSendCompleteOnboardingEmail());
        assertTrue(resource.getInstitutionUpdate().getImported());
        assertNull(resource.getInstitutionType());
        reflectionEqualsByName(users.get(0), resource.getUsers().get(0), "role");
        assertEquals(users.get(0).getRole(), resource.getUsers().get(0).getRole());
        reflectionEqualsByName(importContractDto, resource.getContractImported());
    }

    @Test
    void toOnboardingImportData_null() {
        //given
        OnboardingImportDto onboardingDto = null;
        //when
        OnboardingImportData resource = OnboardingMapperCustom.toOnboardingImportData(INSTITUTION_ID, onboardingDto);
        //then
        assertNull(resource);
    }

    @Test
    void fromDto() {
        //given
        BillingDataDto model = mockInstance(new BillingDataDto());
        //when
        Billing resource = OnboardingMapperCustom.fromDto(model);
        //then
        assertNotNull(resource);
        reflectionEqualsByName(model, resource);
    }

    @Test
    void fromDto_null() {
        //given
        BillingDataDto model = null;
        //when
        Billing resource = OnboardingMapperCustom.fromDto(model);
        //then
        assertNull(resource);
    }

    @Test
    void toOnboardingData() {
        //given
        String productId = PROD_IO;
        List<UserDto> userDtos = List.of(mockInstance(new UserDto()));
        OnboardingDto model = mockInstance(new OnboardingDto());
        BillingDataDto billingDataDto = mockInstance(new BillingDataDto());
        PspDataDto pspDataDto = mockInstance(new PspDataDto());
        List<GeographicTaxonomyDto> geographicTaxonomyDtos = List.of(mockInstance(new GeographicTaxonomyDto()));
        model.setBillingData(billingDataDto);
        model.setUsers(userDtos);
        model.setPspData(pspDataDto);
        model.setGeographicTaxonomies(geographicTaxonomyDtos);
        //when
        OnboardingData resource = OnboardingMapperCustom.toOnboardingData(INSTITUTION_ID, productId, model);
        //then
        assertNotNull(resource);
        assertEquals(model.getUsers().size(), resource.getUsers().size());
        assertEquals(model.getGeographicTaxonomies().size(), resource.getInstitutionUpdate().getGeographicTaxonomies().size());
        assertEquals(INSTITUTION_ID, resource.getInstitutionExternalId());
        assertEquals(productId, resource.getProductId());
        reflectionEqualsByName(billingDataDto, resource.getBilling());
        reflectionEqualsByName(userDtos.get(0), resource.getUsers().get(0), "role");
        assertEquals(userDtos.get(0).getRole(), resource.getUsers().get(0).getRole());
        reflectionEqualsByName(model.getBillingData(), resource.getInstitutionUpdate());
        reflectionEqualsByName(model.getPspData(), resource.getInstitutionUpdate().getPaymentServiceProvider(), "dpoData");
        reflectionEqualsByName(model.getPspData().getDpoData(), resource.getInstitutionUpdate().getDataProtectionOfficer());
        reflectionEqualsByName(geographicTaxonomyDtos.get(0), resource.getInstitutionUpdate().getGeographicTaxonomies().get(0));
        assertEquals(model.getOrigin(), resource.getOrigin());
        assertEquals(model.getInstitutionType(), resource.getInstitutionType());
        assertEquals(model.getPricingPlan(), resource.getPricingPlan());
    }
}
