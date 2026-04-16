package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IvassRestClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IvassRestClient ivassRestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ivassRestClient = new IvassRestClient("http://example.com", "getInsurancesPath", restTemplate);
    }

    @Test
    void getInsurancesZip_shouldReturnByteArray_whenSuccessful() {
        byte[] expectedResponse = new byte[]{1, 2, 3};
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(expectedResponse);

        byte[] result = ivassRestClient.getInsurancesZip();

        assertArrayEquals(expectedResponse, result);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(byte[].class));
    }

    @Test
    void getInsurancesZip_shouldThrowException_whenRestTemplateThrowsException() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> ivassRestClient.getInsurancesZip());
    }
}