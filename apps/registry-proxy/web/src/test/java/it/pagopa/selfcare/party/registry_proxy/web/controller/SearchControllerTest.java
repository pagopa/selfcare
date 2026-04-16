package it.pagopa.selfcare.party.registry_proxy.web.controller;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {SearchController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {SearchController.class, WebTestConfig.class, OnboardingMapperImpl.class})
public class SearchControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SearchService searchService;

  @Test
  void searchInstitutions_shouldReturnOk() throws Exception {
    // Arrange
    SearchServiceInstitution institution = new SearchServiceInstitution();
    institution.setId("test-id");
    institution.setDescription("Test Institution");
    List<SearchServiceInstitution> mockResponse = Collections.singletonList(institution);

    when(searchService.searchInstitution(anyString(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(mockResponse);

    // Act & Assert
    mockMvc.perform(get("/search/institutions")
        .param("searchText", "Test")
        .param("products", "prod-io")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(1)))
      .andExpect(jsonPath("$[0].id", is("test-id")))
      .andExpect(jsonPath("$[0].description", is("Test Institution")));
  }

  @Test
  void searchInstitutions_shouldReturnInternalServerError_onServiceException() throws Exception {
    // Arrange
    when(searchService.searchInstitution(anyString(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenThrow(new RuntimeException("Internal service error"));

    // Act & Assert
    mockMvc.perform(get("/search/institutions")
        .param("searchText", "Test")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$", hasSize(0))); // Expecting an empty list as the body
  }

  @Test
  void searchOnboardingTest() throws Exception {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of("ACTIVE", "PENDING");

    final OnboardingIndexSearch response = new OnboardingIndexSearch();
    response.setTotalPages(1L);
    response.setPage(0L);
    response.setPageSize(15L);
    response.setTotalElements(1L);
    final OnboardingIndex onboardingIndex = new OnboardingIndex();
    onboardingIndex.setOnboardingId("123");
    onboardingIndex.setDescription("Test Onboarding");
    response.setOnboardings(List.of(onboardingIndex));

    when(searchService.searchOnboarding(searchText, products, institutionTypes, statuses, 0L, 15L, "description asc"))
            .thenReturn(response);

    mockMvc.perform(get("/search/onboardings")
                    .param("searchText", searchText)
                    .param("products", String.join(",", products))
                    .param("institutionTypes", String.join(",", institutionTypes))
                    .param("statuses", String.join(",", statuses))
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPages", is(1)))
            .andExpect(jsonPath("$.page", is(0)))
            .andExpect(jsonPath("$.pageSize", is(15)))
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andExpect(jsonPath("$.onboardings", hasSize(1)))
            .andExpect(jsonPath("$.onboardings[0].onboardingId", is("123")))
            .andExpect(jsonPath("$.onboardings[0].description", is("Test Onboarding")));
  }

  @Test
  void searchOnboardingTest_internalServerError() throws Exception {
    when(searchService.searchOnboarding(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("Internal service error"));

    mockMvc.perform(get("/search/onboardings")
                    .param("searchText", "Test")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andReturn().getResponse().getContentAsString();
  }

}
