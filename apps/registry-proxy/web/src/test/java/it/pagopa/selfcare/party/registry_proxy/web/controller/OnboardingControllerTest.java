package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapperImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {OnboardingController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {OnboardingController.class, WebTestConfig.class, PartyRegistryProxyExceptionHandler.class, OnboardingMapperImpl.class})
public class OnboardingControllerTest {

    private static final String BASE_URL = "/onboarding";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SearchService searchService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void indexOnboardingTest() throws Exception {
        final OnboardingIndexResource resource = new OnboardingIndexResource();
        resource.setOnboardingId("123");
        Mockito.when(searchService.indexOnboarding(any())).thenReturn(true);
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/update-index")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(resource)))
                .andExpect(status().isNoContent());
    }

    @Test
    void indexOnboardingTest_withMissingOnboardingId() throws Exception {
        final OnboardingIndexResource resource = new OnboardingIndexResource();
        resource.setDescription("test");
        Mockito.when(searchService.indexOnboarding(any())).thenReturn(true);
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/update-index")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(resource)))
                .andExpect(status().isBadRequest());
    }

}
