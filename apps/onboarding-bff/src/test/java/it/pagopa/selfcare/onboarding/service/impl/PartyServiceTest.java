package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.onboarding.client.PartyProcessRestClient;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingInstitutionRequest;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.api.UserControllerApi;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock PartyProcessRestClient restClient;
    @Mock InstitutionMapper mapper;
    @Mock UserControllerApi userApi;
    @Mock InstitutionControllerApi institutionApi;
    @InjectMocks PartyService service;

    @Test
    void onboardingOrganization_mapsOnceAndForwardsTheExactRequestOnce() {
        OnboardingData data = new OnboardingData();
        OnboardingInstitutionRequest request = new OnboardingInstitutionRequest();
        when(mapper.toOnboardingInstitutionRequest(data)).thenReturn(request);

        service.onboardingOrganization(data);

        verify(mapper).toOnboardingInstitutionRequest(data);
        verify(restClient).onboardingOrganization(request);
        verifyNoMoreInteractions(mapper, restClient);
        verifyNoInteractions(userApi, institutionApi);
    }

    @Test
    void onboardingOrganization_checksRequiredDataBeforeMappingOrCallingDownstream() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.onboardingOrganization(null));

        assertEquals("Onboarding data is required", failure.getMessage());
        verifyNoInteractions(mapper, restClient, userApi, institutionApi);
    }
}
