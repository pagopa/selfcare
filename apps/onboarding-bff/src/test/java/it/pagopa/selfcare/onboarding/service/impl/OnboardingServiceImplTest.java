package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi;
import org.openapi.quarkus.onboarding_json.api.BillingPortalApi;
import org.openapi.quarkus.onboarding_json.api.InternalV1Api;
import org.openapi.quarkus.onboarding_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_json.api.SupportApi;
import org.openapi.quarkus.onboarding_json.api.TokenControllerApi;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingStatus;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {

    @Mock
    private OnboardingControllerApi onboardingApi;

    @Mock
    private BillingPortalApi billingPortalApi;

    @Mock
    private SupportApi supportApi;

    @Mock
    private DocumentContentControllerApi documentContentControllerApi;

    @Mock
    private TokenControllerApi tokenApi;

    @Mock
    private AggregatesControllerApi aggregatesApi;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Mock
    private InternalV1Api internalV1Api;

    @InjectMocks
    private OnboardingServiceImpl onboardingService;

    @Test
    void onboardingWithFilter_blankStatus_passesNullStatusToApi() {
        OnboardingGetResponse expected = new OnboardingGetResponse();
        when(onboardingApi.getOnboardingWithFilter(
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(String.class),
                isNull(OnboardingStatus.class),
                isNull(String.class),
                eq("taxCode"),
                isNull(String.class),
                isNull(String.class))).thenReturn(Uni.createFrom().item(expected));

        OnboardingGetResponse result = onboardingService.onboardingWithFilter("taxCode", " ");

        assertSame(expected, result);
    }

    @Test
    void onboardingWithFilter_invalidStatus_throwsInvalidRequestException() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> onboardingService.onboardingWithFilter("taxCode", "NOT_A_STATUS"));

        assertTrue(exception.getMessage().contains("Allowed values"));
        verifyNoInteractions(onboardingApi);
    }
}
