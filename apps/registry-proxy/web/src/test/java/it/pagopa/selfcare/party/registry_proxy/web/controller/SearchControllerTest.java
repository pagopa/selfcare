package it.pagopa.selfcare.party.registry_proxy.web.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.SearchServiceInstitution;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapperImpl;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {SearchController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {SearchController.class, WebTestConfig.class, OnboardingMapperImpl.class})
public class SearchControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SearchService searchService;

  @BeforeAll
  static void setUp() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @Test
  void searchInstitutionTest() throws Exception {
    final String searchText = "Test Institution";
    final SearchServiceInstitution institution = new SearchServiceInstitution();
    institution.setId("123");
    institution.setDescription("Test Institution");
    institution.setTaxCode("ABC123");
    institution.setParentDescription("Parent Institution");
    institution.setLastModified(OffsetDateTime.parse("2024-01-01T12:00:00Z"));

    when(searchService.searchInstitution(searchText, 100L)).thenReturn(List.of(institution));

    mockMvc.perform(get("/search/institutions")
                    .param("searchText", searchText)
                    .param("top", "100")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is("123")))
            .andExpect(jsonPath("$[0].description", is("Test Institution")))
            .andExpect(jsonPath("$[0].taxCode", is("ABC123")))
            .andExpect(jsonPath("$[0].parentDescription", is("Parent Institution")))
            .andExpect(jsonPath("$[0].lastModified", is("2024-01-01T12:00:00Z")));
  }

  @Test
  void searchInstitutionTest_internalServerError() throws Exception {
    when(searchService.searchInstitution(anyString(), anyLong()))
        .thenThrow(new RuntimeException("Internal service error"));

    mockMvc.perform(get("/search/institutions")
                    .param("searchText", "Test Institution")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void searchOnboardingTest() throws Exception {
    final String searchText = "Test";
    final List<String> products = List.of("prod-io", "prod-pagopa");
    final List<String> institutionTypes = List.of("PA", "GSP");
    final List<String> statuses = List.of("ACTIVE", "PENDING");
    final OffsetDateTime createdFromDate = OffsetDateTime.parse("2024-01-01T00:00:00Z");
    final OffsetDateTime createdToDate = OffsetDateTime.parse("2024-12-31T23:59:59Z");
    final List<String> orderBy = List.of("description_ASC");

    final OnboardingIndexSearch response = new OnboardingIndexSearch();
    response.setTotalPages(1L);
    response.setPage(0L);
    response.setPageSize(15L);
    response.setTotalElements(1L);
    final OnboardingIndex onboardingIndex = new OnboardingIndex();
    onboardingIndex.setOnboardingId("123");
    onboardingIndex.setDescription("Test Onboarding");
    response.setOnboardings(List.of(onboardingIndex));

    when(searchService.searchOnboarding(searchText, products, institutionTypes, statuses, createdFromDate, createdToDate, 0L, 15L, orderBy))
            .thenReturn(response);

    mockMvc.perform(get("/search/onboardings")
                    .param("searchText", searchText)
                    .param("products", String.join(",", products))
                    .param("institutionTypes", String.join(",", institutionTypes))
                    .param("statuses", String.join(",", statuses))
                    .param("createdFromDate", createdFromDate.toString())
                    .param("createdToDate", createdToDate.toString())
                    .param("orderBy", String.join(",", orderBy))
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
    when(searchService.searchOnboarding(anyString(), any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
            .thenThrow(new RuntimeException("Internal service error"));

    mockMvc.perform(get("/search/onboardings")
                    .param("searchText", "Test")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andReturn().getResponse().getContentAsString();
  }
}
