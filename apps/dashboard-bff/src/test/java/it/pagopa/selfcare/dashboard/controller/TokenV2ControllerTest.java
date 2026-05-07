package it.pagopa.selfcare.dashboard.controller;

import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.security.ExchangeTokenServiceV2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TokenV2ControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/v2/token";

    @InjectMocks
    private TokenV2Controller tokenV2Controller;

    @Mock
    private ExchangeTokenServiceV2 exchangeTokenServiceMock;

    @BeforeEach
    void setUp() {
        super.setUp(tokenV2Controller);
    }

    @Test
    void exchange() throws Exception {
        // given
        String institutionId = "inst1";
        String productId = "prod1";
        Mockito.when(exchangeTokenServiceMock.exchange(institutionId, productId, Optional.empty()))
                .thenReturn(new ExchangedToken("token", "urlBO"));
        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get(BASE_URL + "/exchange")
                .param("institutionId", institutionId)
                .param("productId", productId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("token")))
                .andReturn();
        // then
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .exchange(institutionId, productId, Optional.empty());
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }

    @Test
    void exchange_emptyIdentityToken() throws Exception {
        // given
        String institutionId = "inst1";
        String productId = "prod1";
        Mockito.when(exchangeTokenServiceMock.exchange(institutionId, productId, Optional.empty()))
                .thenReturn(new ExchangedToken(null, null));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/exchange")
                        .param("institutionId", institutionId)
                        .param("productId", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        Assertions.assertFalse(content.isEmpty());
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .exchange(institutionId, productId, Optional.empty());
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }

    @Test
    void exchangeBackofficeAdmin() throws Exception {
        // given
        String institutionId = "inst1";
        String productId = "prod1";
        Mockito.when(exchangeTokenServiceMock.exchangeBackofficeAdmin(institutionId, productId, Optional.empty()))
                .thenReturn(new ExchangedToken("token", "urlBO"));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/exchange/back-office/admin")
                        .param("institutionId", institutionId)
                        .param("productId", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        URI resource = objectMapper.readValue(result.getResponse().getContentAsString(), URI.class);
        assertNotNull(resource);
        assertEquals("urlBO", resource.toString());
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .exchangeBackofficeAdmin(institutionId, productId, Optional.empty());
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }

    @Test
    void exchangeBackofficeAdmin_emptyIdentityToken() throws Exception {
        // given
        String institutionId = "inst1";
        String productId = "prod1";
        Mockito.when(exchangeTokenServiceMock.exchangeBackofficeAdmin(institutionId, productId, Optional.empty()))
                .thenReturn(new ExchangedToken("", ""));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/exchange/back-office/admin")
                        .param("institutionId", institutionId)
                        .param("productId", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        URI resource = objectMapper.readValue(result.getResponse().getContentAsString(), URI.class);
        assertNotNull(resource);
        assertEquals("", resource.toString());
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .exchangeBackofficeAdmin(institutionId, productId, Optional.empty());
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }

    @Test
    void billingExchange() throws Exception {
        // given
        String institutionId = "inst1";
        Mockito.when(exchangeTokenServiceMock.retrieveBillingExchangedToken(institutionId))
                .thenReturn(new ExchangedToken("token", "urlBO"));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/exchange/fatturazione")
                        .param("institutionId", institutionId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        URI resource = objectMapper.readValue(result.getResponse().getContentAsString(), URI.class);
        assertNotNull(resource);
        assertEquals("urlBO", resource.toString());
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .retrieveBillingExchangedToken(institutionId);
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }

    @Test
    void billingExchange_emptyIdentityToken() throws Exception {
        // given
        String institutionId = "inst1";
        Mockito.when(exchangeTokenServiceMock.retrieveBillingExchangedToken(institutionId))
                .thenReturn(new ExchangedToken("", ""));
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/exchange/fatturazione")
                        .param("institutionId", institutionId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        URI resource = objectMapper.readValue(result.getResponse().getContentAsString(), URI.class);
        assertNotNull(resource);
        assertEquals("", resource.toString());
        verify(exchangeTokenServiceMock, Mockito.times(1))
                .retrieveBillingExchangedToken(institutionId);
        verifyNoMoreInteractions(exchangeTokenServiceMock);
    }
}