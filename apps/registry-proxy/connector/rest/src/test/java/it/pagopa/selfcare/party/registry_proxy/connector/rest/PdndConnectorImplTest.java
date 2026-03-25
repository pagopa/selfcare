package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.cache.CacheConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PdndRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = {CacheConfig.class})
@TestPropertySource(locations = "classpath:config/pdnd-rest-client.properties")
@EnableCaching
class PdndConnectorImplTest {
    @InjectMocks
    static PdndConnectorImpl pdndConnectorImpl;
    @Mock
    static PdndRestClient pdndRestClient;

    @Test
    void createTokenReturnsValidResponseForValidCredentials() {
        // Given
        String clientAssertion = "validAssertion";
        String clientAssertionType = "validType";
        String grantType = "client_credentials";
        String clientId = "validClientId";
        ClientCredentialsResponse expectedResponse = new ClientCredentialsResponse();
        expectedResponse.setAccessToken("validAccessToken");
        when(pdndRestClient.createToken(any())).thenReturn(expectedResponse);

        // When
        ClientCredentialsResponse actualResponse = pdndConnectorImpl.createToken(clientAssertion, clientAssertionType, grantType, clientId);

        // Then
        assertNotNull(actualResponse);
        assertEquals("validAccessToken", actualResponse.getAccessToken());
    }

}