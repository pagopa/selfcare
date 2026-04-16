package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import feign.FeignException;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDVisuraInfoCamereRawRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PDNDInfoCamereRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PDNDVisuraInfoCamereRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PDNDCacheableServiceTest {

    @Mock
    private TokenProviderVisura tokenProviderVisura;
    @Mock
    private PDNDVisuraInfoCamereRawRestClient pdndVisuraInfoCamereRawRestClient;
    @Mock
    private PDNDVisuraInfoCamereRestClientConfig pdndVisuraInfoCamereRestClientConfig;
    @Mock
    private TokenProvider tokenProviderPDND;
    @Mock
    private PDNDInfoCamereRestClientConfig pdndInfoCamereRestClientConfig;
    @Mock
    private PDNDInfoCamereRestClient pdndInfoCamereRestClient;

    private PDNDCacheableService pdndCacheableService;

    @BeforeEach
    void setup() {
        pdndCacheableService = new PDNDCacheableService(
                tokenProviderVisura,
                pdndVisuraInfoCamereRawRestClient,
                pdndVisuraInfoCamereRestClientConfig,
                pdndInfoCamereRestClient,
                tokenProviderPDND,
                pdndInfoCamereRestClientConfig
        );
    }

    @Test
    void getEncryptedDocument_success() {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAX123";
        String accessToken = "token123";
        String responseBody = "{\"ok\":true}";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = null;
        when(pdndVisuraInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderVisura.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(accessToken);

        when(pdndVisuraInfoCamereRawRestClient.getRawInstitutionDetail(decryptedTax, ("Bearer " + accessToken)))
                .thenReturn(responseBody.getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            utils.when(() -> DataEncryptionUtils.encrypt(responseBody)).thenReturn("ENCRYPTED:" + responseBody);

            String result = pdndCacheableService.getEncryptedDocument(encryptedTax);

            assertThat(result).isEqualTo("ENCRYPTED:" + responseBody);
            utils.verify(() -> DataEncryptionUtils.decrypt(encryptedTax));
            utils.verify(() -> DataEncryptionUtils.encrypt(responseBody));
            verify(tokenProviderVisura).getTokenPdnd(pdndSecretValue);
            verify(pdndVisuraInfoCamereRawRestClient)
                    .getRawInstitutionDetail(decryptedTax, "Bearer " + accessToken);
        }
    }

    @Test
    void getEncryptedDocument_badRequestMapsToResourceNotFound() {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAX123";
        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);

        PdndSecretValue pdndSecretValue = null;

        when(pdndVisuraInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderVisura.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn("tok");

        FeignException.BadRequest badReq = mock(FeignException.BadRequest.class);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            when(pdndVisuraInfoCamereRawRestClient.getRawInstitutionDetail(anyString(), anyString()))
                    .thenThrow(badReq);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> pdndCacheableService.getEncryptedDocument(encryptedTax));

            assertThat(ex.getMessage()).contains(decryptedTax);
        }
    }

    @Test
    void getEncryptedDocument_otherFeignExceptionIsPropagated() {
        String encryptedTax = "ENC";
        String decryptedTax = "TAX999";
        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);

        PdndSecretValue pdndSecretValue = null;

        when(pdndVisuraInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderVisura.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn("tok");

        FeignException generic = mock(FeignException.class);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            when(pdndVisuraInfoCamereRawRestClient.getRawInstitutionDetail(anyString(), anyString()))
                    .thenThrow(generic);

            assertThrows(FeignException.class, () -> pdndCacheableService.getEncryptedDocument(encryptedTax));
        }
    }

    @Test
    void getEncryptedDocument_unexpectedExceptionWrappedAsIllegalArgument() {
        String encryptedTax = "ENC";
        String decryptedTax = "TAX999";
        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = null;

        when(pdndVisuraInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderVisura.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn("tok");

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            when(pdndVisuraInfoCamereRawRestClient.getRawInstitutionDetail(anyString(), anyString()))
                    .thenThrow(new RuntimeException("boom"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pdndCacheableService.getEncryptedDocument(encryptedTax));
            assertThat(ex).hasMessageContaining("Unexpected error while retrieving institution detail");
        }
    }

    @Test
    void getEncryptedPDNDImpresa_success() throws Exception {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = null;

        when(pdndInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderPDND.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(token);

        PDNDImpresa impresa = new PDNDImpresa();
        List<PDNDImpresa> list = Collections.singletonList(impresa);
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(decryptedTax, "Bearer " + token))
                .thenReturn(list);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            utils.when(() -> DataEncryptionUtils.encrypt(anyString()))
                    .thenAnswer(inv -> "ENCRYPTED:" + inv.getArgument(0, String.class));

            String result = pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax);

            assertThat(result).startsWith("ENCRYPTED:");
            verify(tokenProviderPDND).getTokenPdnd(pdndSecretValue);
            verify(pdndInfoCamereRestClient).retrieveInstitutionPdndByTaxCode(decryptedTax, "Bearer " + token);
        }
    }

    @Test
    void getEncryptedPDNDImpresa_exceptionWrappedAsIllegalArgument() {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = null;

        when(pdndInfoCamereRestClientConfig.getPdndSecretValue()).thenReturn(pdndSecretValue);
        when(tokenProviderPDND.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(token);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(anyString(), anyString()))
                    .thenThrow(new RuntimeException("boom"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax));
            assertThat(ex).hasMessageContaining("Unexpected error while retrieving institution");
        }
    }

}