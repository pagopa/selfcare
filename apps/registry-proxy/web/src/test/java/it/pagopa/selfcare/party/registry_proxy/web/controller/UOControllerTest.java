package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.core.InstitutionService;
import it.pagopa.selfcare.party.registry_proxy.core.UOService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyUO;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyUOQueryResult;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.UOMapperImpl;
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
import java.util.Optional;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {UOController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {UOController.class, WebTestConfig.class, PartyRegistryProxyExceptionHandler.class, UOMapperImpl.class})
class UOControllerTest {

    private static final String BASE_URL = "";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @MockBean
    private UOService uoServiceMock;

    @MockBean
    private InstitutionService institutionServiceMock;

    /**
     * Method under test: {@link UOController#findAll(Integer, Integer, Optional)}
     */
    @Test
    void findUOs_defaultInputParams() throws Exception {
        // given
        when(uoServiceMock.findAll(any(), anyInt(), anyInt()))
                .thenReturn(new DummyUOQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
        // then
        verify(uoServiceMock, times(1))
                .findAll(Optional.empty(), 1, 10);
        verifyNoMoreInteractions(uoServiceMock);
    }

    /**
     * Method under test: {@link UOController#findAll(Integer, Integer, Optional)}
     */
    @Test
    void findUOs() throws Exception {
        // given
        final String page = "2";
        final String limit = "2";
        final String taxCodeInvoicing = "taxCodeInvoicing";
        when(uoServiceMock.findAll(any(), anyInt(), anyInt()))
                .thenReturn(new DummyUOQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo")
                        .queryParam("page", page)
                        .queryParam("limit", limit)
                        .queryParam("taxCodeInvoicing", taxCodeInvoicing)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
        // then
        verify(uoServiceMock, times(1))
                .findAll(Optional.of("taxCodeInvoicing"), Integer.parseInt(page), Integer.parseInt(limit));
        verifyNoMoreInteractions(uoServiceMock);
    }

    /**
     * Method under test: {@link UOController#findAll(Integer, Integer, Optional)}
     */
    @Test
    void findUo() throws Exception {
        // given
        final String code = "CODE";
        when(uoServiceMock.findByUnicode(any(), any()))
                .thenReturn(mockInstance(new DummyUO()));
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo/{codiceUniUo}", code)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codiceUniUo", Matchers.matchesPattern("setCodiceUniUo")))
                .andExpect(jsonPath("$.origin", notNullValue()))
                .andExpect(jsonPath("$.codiceIpa", notNullValue()))
                .andExpect(jsonPath("$.denominazioneEnte", notNullValue()))
                .andExpect(jsonPath("$.codiceFiscaleEnte", notNullValue()))
                .andExpect(jsonPath("$.descrizioneUo", notNullValue()));
        // then

        verify(uoServiceMock, times(1))
                .findByUnicode(code, null);
        verifyNoMoreInteractions(uoServiceMock);
    }

    /**
     * Method under test: {@link UOController#findByUnicode(String, List)}
     */
    @Test
    void findUoNotFound() throws Exception {
        // given
        final String code = "CODE";
        when(uoServiceMock.findByUnicode(any(), any()))
                .thenThrow(ResourceNotFoundException.class);
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo/{codiceUniUo}", code)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
        // then
        verify(uoServiceMock, times(1))
                .findByUnicode(code, null);
        verifyNoMoreInteractions(uoServiceMock);
    }

    /**
     * Method under test: {@link UOController#findByUnicode(String, List)}
     */
    @Test
    void findUoWithCategoriesFound() throws Exception {
        // given
        final String code = "CODE";
        List<String> categories = List.of("L4");
        when(uoServiceMock.findByUnicode(any(), eq(categories)))
                .thenReturn(mockInstance(new DummyUO()));
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo/{codiceUniUo}", code)
                        .param("categories","L4")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codiceUniUo", Matchers.matchesPattern("setCodiceUniUo")))
                .andExpect(jsonPath("$.origin", notNullValue()))
                .andExpect(jsonPath("$.codiceIpa", notNullValue()))
                .andExpect(jsonPath("$.denominazioneEnte", notNullValue()))
                .andExpect(jsonPath("$.codiceFiscaleEnte", notNullValue()))
                .andExpect(jsonPath("$.descrizioneUo", notNullValue()));

        verify(uoServiceMock, times(1))
                .findByUnicode(code, categories);
    }

    /**
     * Method under test: {@link UOController#findByUnicode(String, List)}
     */
    @Test
    void findUoWithEmptyMail() throws Exception {
        // given
        final String code = "CODE";
        List<String> categories = List.of("L4");
        var uo = new DummyUO();
        when(uoServiceMock.findByUnicode(any(), eq(categories)))
                .thenReturn(mockInstance(uo));
        uo.setMail1(null);
        var institution = new DummyInstitution();
        institution.setDigitalAddress("test@address.it");
        when(institutionServiceMock.findById(any(), any(), any()))
                .thenReturn(mockInstance(new DummyInstitution()));
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo/{codiceUniUo}", code)
                        .param("categories","L4")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codiceUniUo", Matchers.matchesPattern("setCodiceUniUo")))
                .andExpect(jsonPath("$.origin", notNullValue()))
                .andExpect(jsonPath("$.codiceIpa", notNullValue()))
                .andExpect(jsonPath("$.denominazioneEnte", notNullValue()))
                .andExpect(jsonPath("$.codiceFiscaleEnte", notNullValue()))
                .andExpect(jsonPath("$.descrizioneUo", notNullValue()))
                .andExpect(jsonPath("$.mail1", notNullValue()));

        verify(uoServiceMock, times(1))
                .findByUnicode(code, categories);
    }

    /**
     * Method under test: {@link UOController#findByUnicode(String, List)}
     */
    @Test
    void findUoWithCategoriesNotFound() throws Exception {
        // given
        final String code = "CODE";
        List<String> categories = List.of("L4");
        when(uoServiceMock.findByUnicode(any(), eq(categories)))
                .thenThrow(ResourceNotFoundException.class);
        // when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/uo/{codiceUniUo}", code)
                        .param("categories","L4")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());

        verify(uoServiceMock, times(1))
                .findByUnicode(code, categories);
    }

}