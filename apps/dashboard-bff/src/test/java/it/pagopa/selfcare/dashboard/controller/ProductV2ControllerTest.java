package it.pagopa.selfcare.dashboard.controller;

import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.security.ExchangeTokenServiceV2;
import it.pagopa.selfcare.dashboard.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductV2ControllerTest extends BaseControllerTest {
    private static final String BASE_URL = "/v2/products";

    @InjectMocks
    private ProductV2Controller productV2Controller;
    @Mock
    private ProductService productServiceMock;
    @Mock
    private ExchangeTokenServiceV2 exchangeTokenServiceMock;

    @BeforeEach
    public void setUp() {
        super.setUp(productV2Controller);
    }

    @Test
    void retrieveProductBackoffice() throws Exception {
        // given
        String productId = "prod1";
        String institutionId = "inst1";
        final String identityToken = "identityToken";
        final String backOfficeUrl = "back-office-url#token=";
        when(exchangeTokenServiceMock.exchange(institutionId, productId, Optional.empty()))
                .thenReturn(new ExchangedToken(identityToken, backOfficeUrl + "<IdentityToken>"));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{productId}/back-office", productId)
                        .queryParam("institutionId", institutionId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        URI response = objectMapper.readValue(result.getResponse().getContentAsString(), URI.class);
        assertTrue(response.toString().contains(identityToken));
        assertTrue(response.toString().contains(backOfficeUrl));

        verify(exchangeTokenServiceMock, times(1))
                .exchange(institutionId, productId, Optional.empty());
        verifyNoMoreInteractions(exchangeTokenServiceMock);
        verifyNoInteractions(productServiceMock);
    }

}
