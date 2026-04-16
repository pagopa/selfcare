package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.core.StationService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyPDND;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyPDNDQueryResult;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.StationMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {StationController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {StationController.class, WebTestConfig.class, PartyRegistryProxyExceptionHandler.class, StationMapperImpl.class})
class StationControllerTest {

    private static final String BASE_URL = "/stations";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @MockBean
    private StationService stationServiceMock;

    @Test
    void search() throws Exception {
        // given
        final String search = "search";
        final String page = "2";
        final String limit = "2";
        when(stationServiceMock.search(any(), anyInt(), anyInt()))
                .thenReturn(new DummyPDNDQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
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
                .andExpect(jsonPath("$.items[0].anacEnabled", notNullValue()))
                .andExpect(jsonPath("$.items[0].anacEngaged", notNullValue()))
                .andExpect(jsonPath("$.items[0].taxCode", notNullValue()))
                .andExpect(jsonPath("$.items[0].description", notNullValue()))
                .andExpect(jsonPath("$.items[0].digitalAddress", notNullValue()));
        // then
        verify(stationServiceMock, times(1))
                .search(Optional.of(search), Integer.parseInt(page), Integer.parseInt(limit));
        verifyNoMoreInteractions(stationServiceMock);
    }

    @Test
    void search_defaultInputParams() throws Exception {
        // given
        when(stationServiceMock.search(any(), anyInt(), anyInt()))
                .thenReturn(new DummyPDNDQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", notNullValue()))
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.items[0].id", notNullValue()))
                .andExpect(jsonPath("$.items[0].originId", notNullValue()))
                .andExpect(jsonPath("$.items[0].anacEnabled", notNullValue()))
                .andExpect(jsonPath("$.items[0].anacEngaged", notNullValue()))
                .andExpect(jsonPath("$.items[0].taxCode", notNullValue()))
                .andExpect(jsonPath("$.items[0].description", notNullValue()))
                .andExpect(jsonPath("$.items[0].digitalAddress", notNullValue()));
        // then
        verify(stationServiceMock, times(1))
                .search(Optional.empty(), 1, 10);
        verifyNoMoreInteractions(stationServiceMock);
    }

    @Test
    void findStation() throws Exception {
        // given
        final String taxId = "CODE";
        when(stationServiceMock.findByTaxId(any()))
                .thenReturn(mockInstance(new DummyPDND()));
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{taxId}", taxId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.originId", notNullValue()))
                .andExpect(jsonPath("$.taxCode", notNullValue()))
                .andExpect(jsonPath("$.description", notNullValue()))
                .andExpect(jsonPath("$.digitalAddress", notNullValue()))
                .andExpect(jsonPath("$.anacEngaged", notNullValue()));
        // then

        verify(stationServiceMock, times(1))
                .findByTaxId(taxId);
        verifyNoMoreInteractions(stationServiceMock);
    }

    @Test
    void findStationNotFound() throws Exception {
        // given
        final String taxId = "CODE";
        when(stationServiceMock.findByTaxId(any()))
                .thenThrow(ResourceNotFoundException.class);
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{taxId}", taxId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
        // then
        verify(stationServiceMock, times(1))
                .findByTaxId(taxId);
        verifyNoMoreInteractions(stationServiceMock);
    }
}