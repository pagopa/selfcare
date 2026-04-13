package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.Institution;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.controller.request.CompanyOnboardingUserDto;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingProductDto;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResource;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.RecipientCodeStatus;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;

@ExtendWith(MockitoExtension.class)
class InstitutionV2ControllerTest {

    @Mock
    InstitutionService institutionService;
    @Mock
    UserService userService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    InstitutionMapper institutionMapper;

    @InjectMocks
    InstitutionV2Controller institutionV2Controller;

    @Test
    void onboarding_callsValidationAndService() {
        OnboardingProductDto request = new OnboardingProductDto();
        request.setTaxCode("taxCode");
        request.setProductId("productId");
        OnboardingData entity = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);

        Response response = institutionV2Controller.onboarding(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(institutionService).validateOnboardingByProductOrInstitutionTaxCode("taxCode", "productId");
        verify(institutionService).onboardingProductV2(entity);
    }

    @Test
    void getInstitution_mapsServiceResponse() {
        Institution institution = new Institution();
        InstitutionResource resource = new InstitutionResource();
        when(institutionService.getByFilters("prod", "tax", "origin", "originId", "sub")).thenReturn(List.of(institution));
        when(institutionMapper.toResource(institution)).thenReturn(resource);

        List<InstitutionResource> result = institutionV2Controller.getInstitution("prod", "tax", "origin", "originId", "sub");

        assertEquals(1, result.size());
        assertSame(resource, result.get(0));
    }

    @Test
    void checkRecipientCode_mapsStatus() {
        org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus serviceStatus =
                org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus.ACCEPTED;
        when(institutionService.checkRecipientCode("originId", "recipientCode")).thenReturn(serviceStatus);
        when(onboardingMapper.toRecipientCodeStatus(serviceStatus)).thenReturn(RecipientCodeStatus.ACCEPTED);

        RecipientCodeStatus result = institutionV2Controller.checkRecipientCode("originId", "recipientCode");

        assertEquals(RecipientCodeStatus.ACCEPTED, result);
    }

    @Test
    void onboardingUsers_callsService() {
        CompanyOnboardingUserDto request = new CompanyOnboardingUserDto();
        OnboardingData entity = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);

        institutionV2Controller.onboardingUsers(request);

        verify(institutionService).onboardingUsersPgFromIcAndAde(entity);
    }

    @Test
    void getOnboardingsInfo_returnsServiceResponse() {
        OnboardingGetResponse expected = new OnboardingGetResponse();
        when(institutionService.getOnboardingWithFilter("taxCode", "ACTIVE")).thenReturn(expected);

        OnboardingGetResponse result = institutionV2Controller.getOnboardingsInfo("taxCode", "ACTIVE");

        assertSame(expected, result);
    }
}
