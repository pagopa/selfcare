package it.pagopa.selfcare.mscore.web.controller;

import it.pagopa.selfcare.mscore.core.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@ContextConfiguration(classes = {OnboardingController.class})
@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {
    @InjectMocks
    private OnboardingController onboardingController;

    @Mock
    private OnboardingService onboardingService;

    @BeforeEach
    void resetContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Method under test: {@link OnboardingController#verifyOnboardingInfo(String, String, String)}
     */
    @Test
    void shouldVerifyOnboardingInfoBySubunit() throws Exception {
        doNothing().when(onboardingService).verifyOnboardingInfoSubunit(any(), any(), any());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .head("/onboarding/")
                .queryParam("taxCode", "42")
                .queryParam("productId", "42");
        ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(onboardingController)
                .build()
                .perform(requestBuilder);
        actualPerformResult.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    /**
     * Method under test: {@link OnboardingController#verifyOnboardingInfoByFilters(String, String, String, String, String, String)}
     */
    @Test
    void shouldVerifyOnboardingInfoByFiltersSuccess() throws Exception {
        doNothing().when(onboardingService).verifyOnboardingInfoByFilters(any());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .head("/onboarding/verify")
                .queryParam("origin", "42")
                .queryParam("originId", "42")
                .queryParam("productId", "42");
        ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(onboardingController)
                .build()
                .perform(requestBuilder);
        actualPerformResult.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    /**
     * Method under test: {@link OnboardingController#verifyOnboardingInfo(String, String)}
     */
    @Test
    void shouldVerifyOnboardingInfoByExternalId() throws Exception {
        doNothing().when(onboardingService).verifyOnboardingInfo(any(), any());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .head("/onboarding/institution/{externalId}/products/{productId}", "42", "42");
        ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(onboardingController)
                .build()
                .perform(requestBuilder);
        actualPerformResult.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    }

