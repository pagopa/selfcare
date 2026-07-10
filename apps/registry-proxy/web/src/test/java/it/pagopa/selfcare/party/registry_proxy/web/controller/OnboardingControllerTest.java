package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapperImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        OffsetDateTime activatedAt = OffsetDateTime.now();
        final OnboardingIndexResource resource = new OnboardingIndexResource();
        resource.setOnboardingId("123");
        resource.setStatus(OnboardingStatus.COMPLETED.name());
        resource.setActivatedAt(activatedAt);

        Mockito.when(searchService.indexOnboarding(any())).thenReturn(true);

        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/update-index")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(resource)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<OnboardingIndex> captor = ArgumentCaptor.forClass(OnboardingIndex.class);
        Mockito.verify(searchService).indexOnboarding(captor.capture());

        assertEquals(activatedAt, captor.getValue().getStatusUpdatedAt());
    }

    @Test
    void indexOnboardingTest_deletedStatus() throws Exception {
        OffsetDateTime deletedAt = OffsetDateTime.now();

        OnboardingIndexResource resource = new OnboardingIndexResource();
        resource.setOnboardingId("123");
        resource.setStatus(OnboardingStatus.DELETED.name());
        resource.setDeletedAt(deletedAt);

        Mockito.when(searchService.indexOnboarding(any())).thenReturn(true);

        mvc.perform(MockMvcRequestBuilders
                  .post(BASE_URL + "/update-index")
                  .contentType(APPLICATION_JSON_VALUE)
                  .accept(APPLICATION_JSON_VALUE)
                  .content(mapper.writeValueAsString(resource)))
            .andExpect(status().isNoContent());

        ArgumentCaptor<OnboardingIndex> captor = ArgumentCaptor.forClass(OnboardingIndex.class);
        Mockito.verify(searchService, Mockito.atLeastOnce()).indexOnboarding(captor.capture());

        assertEquals(deletedAt, captor.getValue().getStatusUpdatedAt());
    }

    @Test
    void indexOnboardingTest_pendingStatus() throws Exception {
        OnboardingIndexResource resource = new OnboardingIndexResource();
        resource.setOnboardingId("123");
        resource.setStatus(OnboardingStatus.PENDING.name());

        Mockito.when(searchService.indexOnboarding(any())).thenReturn(true);

        mvc.perform(MockMvcRequestBuilders
                  .post(BASE_URL + "/update-index")
                  .contentType(APPLICATION_JSON_VALUE)
                  .accept(APPLICATION_JSON_VALUE)
                  .content(mapper.writeValueAsString(resource)))
            .andExpect(status().isNoContent());

        ArgumentCaptor<OnboardingIndex> captor = ArgumentCaptor.forClass(OnboardingIndex.class);
        Mockito.verify(searchService, Mockito.atLeastOnce()).indexOnboarding(captor.capture());

        assertNull(captor.getValue().getStatusUpdatedAt());
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
