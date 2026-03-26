package it.pagopa.selfcare.onboarding.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.core.ProductService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.OriginResponse;
import it.pagopa.selfcare.onboarding.web.model.mapper.ProductMapper;
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
}
