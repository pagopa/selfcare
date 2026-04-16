package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.core.CategoryService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.handler.PartyRegistryProxyExceptionHandler;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyCategory;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyCategoryQueryResult;
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

@WebMvcTest(value = {CategoryController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {CategoryController.class, WebTestConfig.class, PartyRegistryProxyExceptionHandler.class})
class CategoryControllerTest {

    private static final String BASE_URL = "";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @MockBean
    private CategoryService categoryServiceMock;


    @Test
    void findCategories_defaultInputParams() throws Exception {
        // given
        when(categoryServiceMock.search(any(), anyInt(), anyInt()))
                .thenReturn(new DummyCategoryQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/categories")
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.items[0].code", notNullValue()))
                .andExpect(jsonPath("$.items[0].name", notNullValue()))
                .andExpect(jsonPath("$.items[0].kind", notNullValue()))
                .andExpect(jsonPath("$.items[0].origin", notNullValue()));
        // then
        verify(categoryServiceMock, times(1))
                .search(Optional.empty(), 1, 10);
        verifyNoMoreInteractions(categoryServiceMock);
    }


    @Test
    void findCategories() throws Exception {
        // given
        final Origin origin = Origin.MOCK;
        final String page = "2";
        final String limit = "2";
        when(categoryServiceMock.search(any(), anyInt(), anyInt()))
                .thenReturn(new DummyCategoryQueryResult());
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/categories")
                .queryParam("origin", origin.toString())
                .queryParam("page", page)
                .queryParam("limit", limit)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.items[0].code", notNullValue()))
                .andExpect(jsonPath("$.items[0].name", notNullValue()))
                .andExpect(jsonPath("$.items[0].kind", notNullValue()))
                .andExpect(jsonPath("$.items[0].origin", notNullValue()));
        // then
        verify(categoryServiceMock, times(1))
                .search(Optional.of(origin), Integer.parseInt(page), Integer.parseInt(limit));
        verifyNoMoreInteractions(categoryServiceMock);
    }

    @Test
    void findCategory() throws Exception {
        // given
        final Origin origin = Origin.IPA;
        final String code = "code";
        when(categoryServiceMock.findById(any(), any()))
                .thenReturn(mockInstance(new DummyCategory()));
        // when
        mvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/origins/{origin}/categories/{code}", origin, code)
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.name", notNullValue()))
                .andExpect(jsonPath("$.kind", notNullValue()))
                .andExpect(jsonPath("$.origin", notNullValue()));
        // then
        verify(categoryServiceMock, times(1))
                .findById(code, origin);
        verifyNoMoreInteractions(categoryServiceMock);
    }

}