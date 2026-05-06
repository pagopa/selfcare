package it.pagopa.selfcare.party.registry_proxy.web.controller;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.core.InstitutionService;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyInstitutionQueryResult;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(
    value = {InstitutionController.class},
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(
    classes = {
      InstitutionController.class,
      WebTestConfig.class,
      PartyRegistryProxyExceptionHandler.class
    })
class InstitutionControllerTest {

  private static final String BASE_URL = "/institutions";

  @Autowired protected MockMvc mvc;

  @Autowired protected ObjectMapper mapper;

  @MockBean private InstitutionService institutionServiceMock;

  @MockBean private SearchService searchService;

  @Test
  void search() throws Exception {
    // given
    final String search = "search";
    final String page = "2";
    final String limit = "2";
    when(institutionServiceMock.search(any(), anyInt(), anyInt()))
        .thenReturn(new DummyInstitutionQueryResult());
    // when
    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL)
                .queryParam("search", search)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", notNullValue()))
        .andExpect(jsonPath("$.items", notNullValue()))
        .andExpect(jsonPath("$.items[0].id", notNullValue()))
        .andExpect(jsonPath("$.items[0].originId", notNullValue()))
        .andExpect(jsonPath("$.items[0].o", notNullValue()))
        .andExpect(jsonPath("$.items[0].ou", notNullValue()))
        .andExpect(jsonPath("$.items[0].aoo", notNullValue()))
        .andExpect(jsonPath("$.items[0].taxCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].category", notNullValue()))
        .andExpect(jsonPath("$.items[0].description", notNullValue()))
        .andExpect(jsonPath("$.items[0].digitalAddress", notNullValue()))
        .andExpect(jsonPath("$.items[0].address", notNullValue()))
        .andExpect(jsonPath("$.items[0].zipCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].origin", notNullValue()))
        .andExpect(jsonPath("$.items[0].istatCode", notNullValue()));
    // then
    verify(institutionServiceMock, times(1))
        .search(Optional.of(search), Integer.parseInt(page), Integer.parseInt(limit));
    verifyNoMoreInteractions(institutionServiceMock);
  }

  @Test
  void searchFiltered() throws Exception {
    // given
    final String search = "search";
    final String page = "2";
    final String limit = "2";
    final String categories = "cat1,cat2,cat3";
    when(institutionServiceMock.search(any(), any(), anyInt(), anyInt()))
        .thenReturn(new DummyInstitutionQueryResult());
    // when
    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL)
                .queryParam("search", search)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .queryParam("categories", categories)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", notNullValue()))
        .andExpect(jsonPath("$.items", notNullValue()))
        .andExpect(jsonPath("$.items[0].id", notNullValue()))
        .andExpect(jsonPath("$.items[0].originId", notNullValue()))
        .andExpect(jsonPath("$.items[0].o", notNullValue()))
        .andExpect(jsonPath("$.items[0].ou", notNullValue()))
        .andExpect(jsonPath("$.items[0].aoo", notNullValue()))
        .andExpect(jsonPath("$.items[0].taxCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].category", notNullValue()))
        .andExpect(jsonPath("$.items[0].description", notNullValue()))
        .andExpect(jsonPath("$.items[0].digitalAddress", notNullValue()))
        .andExpect(jsonPath("$.items[0].address", notNullValue()))
        .andExpect(jsonPath("$.items[0].zipCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].origin", notNullValue()))
        .andExpect(jsonPath("$.items[0].istatCode", notNullValue()));
    // then
    verify(institutionServiceMock, times(1))
        .search(Optional.of(search), categories, Integer.parseInt(page), Integer.parseInt(limit));
    verifyNoMoreInteractions(institutionServiceMock);
  }

  @Test
  void search_defaultInputParams() throws Exception {
    // given
    when(institutionServiceMock.search(any(), anyInt(), anyInt()))
        .thenReturn(new DummyInstitutionQueryResult());
    // when
    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", notNullValue()))
        .andExpect(jsonPath("$.items", notNullValue()))
        .andExpect(jsonPath("$.items[0].id", notNullValue()))
        .andExpect(jsonPath("$.items[0].originId", notNullValue()))
        .andExpect(jsonPath("$.items[0].o", notNullValue()))
        .andExpect(jsonPath("$.items[0].ou", notNullValue()))
        .andExpect(jsonPath("$.items[0].aoo", notNullValue()))
        .andExpect(jsonPath("$.items[0].taxCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].category", notNullValue()))
        .andExpect(jsonPath("$.items[0].description", notNullValue()))
        .andExpect(jsonPath("$.items[0].digitalAddress", notNullValue()))
        .andExpect(jsonPath("$.items[0].address", notNullValue()))
        .andExpect(jsonPath("$.items[0].zipCode", notNullValue()))
        .andExpect(jsonPath("$.items[0].origin", notNullValue()))
        .andExpect(jsonPath("$.items[0].istatCode", notNullValue()));
    // then
    verify(institutionServiceMock, times(1)).search(Optional.empty(), 1, 10);
    verifyNoMoreInteractions(institutionServiceMock);
  }

  @Test
  void findInstitutionFiltered() throws Exception {
    // given
    final String id = "id";
    final Origin origin = Origin.IPA;
    final String categories = "cat1,cat2,cat3";
    final List<String> categoriesMatcher = List.of("cat1", "cat2", "cat3");
    when(institutionServiceMock.findById(any(), any(), any()))
        .thenReturn(mockInstance(new DummyInstitution()));
    // when
    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/{id}", id)
                .queryParam("origin", origin.toString())
                .queryParam("categories", categories)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.originId", notNullValue()))
        .andExpect(jsonPath("$.o", notNullValue()))
        .andExpect(jsonPath("$.ou", notNullValue()))
        .andExpect(jsonPath("$.aoo", notNullValue()))
        .andExpect(jsonPath("$.taxCode", notNullValue()))
        .andExpect(jsonPath("$.category", notNullValue()))
        .andExpect(jsonPath("$.description", notNullValue()))
        .andExpect(jsonPath("$.digitalAddress", notNullValue()))
        .andExpect(jsonPath("$.address", notNullValue()))
        .andExpect(jsonPath("$.zipCode", notNullValue()))
        .andExpect(jsonPath("$.origin", notNullValue()))
        .andExpect(jsonPath("$.istatCode", notNullValue()));
    // then
    verify(institutionServiceMock, times(1)).findById(id, Optional.of(origin), categoriesMatcher);
    verifyNoMoreInteractions(institutionServiceMock);
  }

  @Test
  void findInstitution_defaultInputParams() throws Exception {
    // given
    final String id = "id";
    final List<String> categoriesMatcher = Collections.emptyList();
    when(institutionServiceMock.findById(any(), any(), any()))
        .thenReturn(mockInstance(new DummyInstitution()));
    // when
    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/{id}", id)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.originId", notNullValue()))
        .andExpect(jsonPath("$.o", notNullValue()))
        .andExpect(jsonPath("$.ou", notNullValue()))
        .andExpect(jsonPath("$.aoo", notNullValue()))
        .andExpect(jsonPath("$.taxCode", notNullValue()))
        .andExpect(jsonPath("$.category", notNullValue()))
        .andExpect(jsonPath("$.description", notNullValue()))
        .andExpect(jsonPath("$.digitalAddress", notNullValue()))
        .andExpect(jsonPath("$.address", notNullValue()))
        .andExpect(jsonPath("$.zipCode", notNullValue()))
        .andExpect(jsonPath("$.origin", notNullValue()))
        .andExpect(jsonPath("$.istatCode", notNullValue()));
    // then
    verify(institutionServiceMock, times(1))
        .findById(id, Optional.of(Origin.IPA), categoriesMatcher);
    verifyNoMoreInteractions(institutionServiceMock);
  }

  @Test
  void searchIpaInstitutions_shouldReturnOk() throws Exception {
    IpaInstitution institution = new IpaInstitution();
    institution.setId("ipa-1");
    institution.setDescription("Comune di Roma");
    institution.setTaxCode("00100000001");
    institution.setOrigin(Origin.IPA);
    institution.setCategory("L6");

    IpaInstitutionSearchResult result = new IpaInstitutionSearchResult();
    result.setInstitutions(List.of(institution));
    result.setTotalElements(1L);

    when(searchService.searchIpaInstitutions(anyString(), isNull(), anyInt(), anyInt()))
        .thenReturn(result);

    mvc.perform(
            get("/institutions/ipa")
                .param("searchText", "Roma")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", is(1)))
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id", is("ipa-1")))
        .andExpect(jsonPath("$.items[0].description", is("Comune di Roma")));
  }

  @Test
  void searchIpaInstitutions_withCategory_shouldReturnOk() throws Exception {
    IpaInstitutionSearchResult result = new IpaInstitutionSearchResult();
    result.setInstitutions(List.of());
    result.setTotalElements(0L);

    when(searchService.searchIpaInstitutions(anyString(), eq("L6"), anyInt(), anyInt()))
        .thenReturn(result);

    mvc.perform(
            get("/institutions/ipa")
                .param("searchText", "Test")
                .param("category", "L6")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", is(0)))
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void searchIpaInstitutions_shouldReturnInternalServerError_onException() throws Exception {
    when(searchService.searchIpaInstitutions(anyString(), any(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("Internal service error"));

    mvc.perform(
            get("/institutions/ipa")
                .param("searchText", "Test")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }
}
