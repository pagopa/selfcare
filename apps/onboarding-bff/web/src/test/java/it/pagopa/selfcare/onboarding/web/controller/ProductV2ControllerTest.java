package it.pagopa.selfcare.onboarding.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.core.ProductService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.OriginResponse;
import it.pagopa.selfcare.onboarding.web.model.mapper.ProductMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {ProductV2Controller.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {ProductV2Controller.class, WebTestConfig.class})
class ProductV2ControllerTest {

    private static final String BASE_URL = "/v2/product";

    @Autowired
    protected MockMvc mvc;

    @MockBean
    private ProductService productServiceMock;

    @MockBean
    private ProductMapper productMapperMock;

    @InjectMocks
    private ProductV2Controller productV2Controller;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    void getOriginsTest_success() throws Exception {
        // given
        String productId = "productId-123";
        String sanitized = Encode.forJava(productId);

        OriginResult originResult = new OriginResult();
        OriginResponse originResponse = new OriginResponse();


        when(productServiceMock.getOrigins(sanitized)).thenReturn(originResult);
        when(productMapperMock.toOriginResponse(originResult)).thenReturn(originResponse);

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.get(BASE_URL)
                                .param("productId", productId)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();

        // then
        OriginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OriginResponse.class
        );

        assertNotNull(response);
        verify(productServiceMock, times(1)).getOrigins(sanitized);
        verify(productMapperMock, times(1)).toOriginResponse(originResult);
        verifyNoMoreInteractions(productServiceMock, productMapperMock);
    }

    @Test
    void getOriginsTest_sanitizesProductId() throws Exception {
        // given
        String rawProductId = "<script>";
        String sanitized = Encode.forJava(rawProductId);

        OriginResult originResult = new OriginResult();
        OriginResponse originResponse = new OriginResponse();

        when(productServiceMock.getOrigins(anyString())).thenReturn(originResult);
        when(productMapperMock.toOriginResponse(originResult)).thenReturn(originResponse);

        // when
        mvc.perform(
                        MockMvcRequestBuilders.get(BASE_URL)
                                .param("productId", rawProductId)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        // then
        verify(productServiceMock, times(1)).getOrigins(sanitized);
        verify(productMapperMock, times(1)).toOriginResponse(originResult);
        verifyNoMoreInteractions(productServiceMock, productMapperMock);
    }

    @Test
    void getOriginsTest_missingProductId_badRequest() throws Exception {
        // when
        mvc.perform(
                        MockMvcRequestBuilders.get(BASE_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        // then
        verifyNoInteractions(productServiceMock, productMapperMock);
    }

    @Test
    void getRequiredDocuments_success() throws Exception {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        RequiredDocumentModel doc = new RequiredDocumentModel();
        doc.setId("doc-1");
        doc.setName("Statuto");
        doc.setRequired(true);

        when(productServiceMock.getRequiredDocuments(productId, institutionType, origin)).thenReturn(List.of(doc));

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{productId}/required-documents", productId)
                        .param("institutionType", institutionType)
                        .param("origin", origin)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        // then
        List<RequiredDocumentModel> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals("doc-1", response.get(0).getId());
        verify(productServiceMock, times(1)).getRequiredDocuments(productId, institutionType, origin);
        verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void getRequiredDocuments_emptyList() throws Exception {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productServiceMock.getRequiredDocuments(productId, institutionType, origin)).thenReturn(List.of());

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{productId}/required-documents", productId)
                        .param("institutionType", institutionType)
                        .param("origin", origin)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        // then
        List<RequiredDocumentModel> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    void isRequiredDocumentsEnabled_returnsTrue() throws Exception {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productServiceMock.isRequiredDocumentsEnabled(productId, institutionType, origin)).thenReturn(true);

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{productId}/required-documents/enabled", productId)
                        .param("institutionType", institutionType)
                        .param("origin", origin)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        // then
        Assertions.assertTrue(Boolean.parseBoolean(result.getResponse().getContentAsString()));
        verify(productServiceMock, times(1)).isRequiredDocumentsEnabled(productId, institutionType, origin);
        verifyNoMoreInteractions(productServiceMock);
    }

    @Test
    void isRequiredDocumentsEnabled_returnsFalse() throws Exception {
        // given
        String productId = "prod-test";
        String institutionType = "PA";
        String origin = "IPA";

        when(productServiceMock.isRequiredDocumentsEnabled(productId, institutionType, origin)).thenReturn(false);

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{productId}/required-documents/enabled", productId)
                        .param("institutionType", institutionType)
                        .param("origin", origin)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        // then
        Assertions.assertFalse(Boolean.parseBoolean(result.getResponse().getContentAsString()));
    }

}
