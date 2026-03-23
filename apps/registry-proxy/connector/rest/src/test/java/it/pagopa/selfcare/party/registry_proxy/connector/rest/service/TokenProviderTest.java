package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.PdndConnectorImpl;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.JwtConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.TokenType;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.PdndVisuraConnectorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TokenProviderTest {

    @InjectMocks
    TokenProviderPDND tokenProviderPDND;

    @InjectMocks
    TokenProviderVisura tokenProviderVisura;
    @Mock
    AssertionGenerator assertionGenerator;

    @Mock
    AssertionGeneratorVisura assertionGeneratorVisura;
    @Mock
    PdndConnectorImpl pdndClient;

    @Mock
    PdndVisuraConnectorImpl pdndVisuraClient;

    @Test
    void getTokenPdndSuccess() {
        // given
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder()
                .clientId("clientId")
                .secretKey("secretKey")
                .build();
        JwtConfig jwtConfig = JwtConfig.builder()
                .audience("audience")
                .issuer("issuer")
                .kid("kid")
                .purposeId("purposeId")
                .subject("subject")
                .build();
        pdndSecretValue.setJwtConfig(jwtConfig);

        ClientCredentialsResponse response = new ClientCredentialsResponse();
        response.setAccessToken("accessToken");
        response.setExpiresIn(3600);
        response.setTokenType(TokenType.BEARER);

        // when
        when(assertionGenerator.generateClientAssertion(jwtConfig, pdndSecretValue.getSecretKey()))
                .thenReturn("clientAssertion");
        when(pdndClient.createToken(any(), any(), any(), any()))
                .thenReturn(response);

        ClientCredentialsResponse token = tokenProviderPDND.getTokenPdnd(pdndSecretValue);
        // then
        assertEquals("accessToken", token.getAccessToken());
        assertEquals(3600, token.getExpiresIn());
        assertEquals(TokenType.BEARER, token.getTokenType());
        verify(assertionGenerator, times(1))
                .generateClientAssertion(jwtConfig, pdndSecretValue.getSecretKey());
        verifyNoMoreInteractions(assertionGenerator);
    }

    @Test
    void getTokenVisuraPdndSuccess() {
        // given
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder()
                .clientId("clientId")
                .secretKey("secretKey")
                .build();
        JwtConfig jwtConfig = JwtConfig.builder()
                .audience("audience")
                .issuer("issuer")
                .kid("kid")
                .purposeId("purposeId")
                .subject("subject")
                .build();
        pdndSecretValue.setJwtConfig(jwtConfig);

        ClientCredentialsResponse response = new ClientCredentialsResponse();
        response.setAccessToken("accessToken");
        response.setExpiresIn(3600);
        response.setTokenType(TokenType.BEARER);

        // when
        when(assertionGeneratorVisura.generateClientAssertion(jwtConfig, pdndSecretValue.getSecretKey()))
                .thenReturn("clientAssertion");
        when(pdndVisuraClient.createToken(any(), any(), any(), any()))
                .thenReturn(response);

        ClientCredentialsResponse token = tokenProviderVisura.getTokenPdnd(pdndSecretValue);
        // then
        assertEquals("accessToken", token.getAccessToken());
        assertEquals(3600, token.getExpiresIn());
        assertEquals(TokenType.BEARER, token.getTokenType());
        verify(assertionGeneratorVisura, times(1))
                .generateClientAssertion(jwtConfig, pdndSecretValue.getSecretKey());
        verifyNoMoreInteractions(assertionGeneratorVisura);
    }
}