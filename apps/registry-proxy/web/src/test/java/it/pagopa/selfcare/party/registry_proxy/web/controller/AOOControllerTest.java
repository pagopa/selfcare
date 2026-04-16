package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.core.AOOService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyAOO;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyAOOQueryResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {AOOController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {AOOController.class, WebTestConfig.class, PartyRegistryProxyExceptionHandler.class})
class AOOControllerTest {

    private static final String BASE_URL = "";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @MockBean
    private AOOService aooServiceMock;


    @Test
    void findAOOs_defaultInputParams() throws Exception {
        // given
        when(aooServiceMock.findAll(anyInt(), anyInt()))
                .thenReturn(new DummyAOOQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/aoo")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
        // then
        verify(aooServiceMock, times(1))
                .findAll(1, 10);
        verifyNoMoreInteractions(aooServiceMock);
    }


    @Test
    void findAOOs() throws Exception {
        // given
        final String page = "2";
        final String limit = "2";
        when(aooServiceMock.findAll(anyInt(), anyInt()))
                .thenReturn(new DummyAOOQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/aoo")
                .queryParam("page", page)
                .queryParam("limit", limit)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
        // then
        verify(aooServiceMock, times(1))
                .findAll(Integer.parseInt(page), Integer.parseInt(limit));
        verifyNoMoreInteractions(aooServiceMock);
    }

    @Test
    void findAOO() throws Exception {
        // given
        final String code = "CODE";
        when(aooServiceMock.findByUnicode(any(), any()))
                .thenReturn(mockInstance(new DummyAOO()));
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/aoo/{codiceUniAoo}", code)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codiceUniAoo", Matchers.matchesPattern("setCodiceUniAoo")))
                .andExpect(jsonPath("$.origin", notNullValue()))
                .andExpect(jsonPath("$.codAoo", notNullValue()))
                .andExpect(jsonPath("$.codiceIpa", notNullValue()))
                .andExpect(jsonPath("$.denominazioneEnte", notNullValue()))
                .andExpect(jsonPath("$.codiceFiscaleEnte", notNullValue()))
                .andExpect(jsonPath("$.denominazioneAoo", notNullValue()));
        // then

        verify(aooServiceMock, times(1))
                .findByUnicode(code, null);
        verifyNoMoreInteractions(aooServiceMock);
    }

    @Test
    void findAOONotFound() throws Exception {
        // given
        final String code = "CODE";
        when(aooServiceMock.findByUnicode(any(), any()))
                .thenThrow(ResourceNotFoundException.class);
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/aoo/{codiceUniAoo}", code)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
        // then
        verify(aooServiceMock, times(1))
                .findByUnicode(code, null);
        verifyNoMoreInteractions(aooServiceMock);
    }

    @Test
    void findAooWithCategoriesFound() throws Exception {
        // given
        final String code = "CODE";
        List<String> categories = List.of("L4");
        when(aooServiceMock.findByUnicode(any(), eq(categories)))
                .thenReturn(mockInstance(new DummyAOO()));
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/aoo/{codiceUniAoo}", code)
                        .param("categories","L4")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codiceUniAoo", Matchers.matchesPattern("setCodiceUniAoo")))
                .andExpect(jsonPath("$.origin", notNullValue()))
                .andExpect(jsonPath("$.codAoo", notNullValue()))
                .andExpect(jsonPath("$.codiceIpa", notNullValue()))
                .andExpect(jsonPath("$.denominazioneEnte", notNullValue()))
                .andExpect(jsonPath("$.codiceFiscaleEnte", notNullValue()))
                .andExpect(jsonPath("$.denominazioneAoo", notNullValue()));

        verify(aooServiceMock, times(1))
                .findByUnicode(code, categories);
    }

    @Test
    void findAOOWithCategoriesNotFound() throws Exception {
        // given
        final String code = "CODE";
        List<String> categories = List.of("L4");
        when(aooServiceMock.findByUnicode(any(), eq(categories)))
                .thenThrow(ResourceNotFoundException.class);
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/aoo/{codiceUniAoo}", code)
                        .param("categories","L4")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());

        verify(aooServiceMock, times(1))
                .findByUnicode(code, categories);
    }

}