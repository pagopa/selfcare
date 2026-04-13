package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.controller.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.service.UserInstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenV2ControllerTest {

    @Mock
    TokenService tokenService;
    @Mock
    UserService userService;
    @Mock
    UserInstitutionService userInstitutionService;
    @Mock
    OnboardingMapper onboardingMapper;

    @InjectMocks
    TokenV2Controller tokenV2Controller;

    @Test
    void verifyOnboarding_returnsMappedResult() {
        OnboardingData onboardingData = new OnboardingData();
        OnboardingVerify expected = new OnboardingVerify();
        when(tokenService.verifyOnboarding("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingVerify(onboardingData)).thenReturn(expected);

        OnboardingVerify result = tokenV2Controller.verifyOnboarding("42");

        assertSame(expected, result);
    }

    @Test
    void retrieveOnboardingRequest_returnsMappedResult() {
        OnboardingData onboardingData = new OnboardingData();
        OnboardingRequestResource expected = new OnboardingRequestResource();
        when(tokenService.getOnboardingWithUserInfo("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingRequestResource(onboardingData)).thenReturn(expected);

        OnboardingRequestResource result = tokenV2Controller.retrieveOnboardingRequest("42");

        assertSame(expected, result);
    }

    @Test
    void approveOnboarding_delegatesToService() {
        tokenV2Controller.approveOnboarding("42");

        verify(tokenService).approveOnboarding("42");
    }

    @Test
    void rejectOnboarding_delegatesToService() {
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");

        tokenV2Controller.rejectOnboarding("42", request);

        verify(tokenService).rejectOnboarding("42", "reason");
    }

    @Test
    void deleteOnboarding_returnsNoContent() {
        Response response = tokenV2Controller.deleteOnboarding("42");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(tokenService).rejectOnboarding("42", "REJECTED_BY_USER");
    }

    @Test
    void getContract_returnsBinaryResponse() {
        BinaryData contract = new BinaryData("contract.pdf", "content".getBytes());
        when(tokenService.getContract("42")).thenReturn(contract);

        Response response = tokenV2Controller.getContract("42");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("attachment; filename=contract.pdf", response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
    }
}
