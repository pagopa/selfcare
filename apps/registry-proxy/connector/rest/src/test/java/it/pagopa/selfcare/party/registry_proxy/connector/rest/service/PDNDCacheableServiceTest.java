package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PDNDCacheableServiceTest {

    @Mock
    private TokenProvider tokenProviderPDND;
    @Mock
    private PDNDInfoCamereRestClient pdndInfoCamereRestClient;

    private PDNDCacheableService pdndCacheableService;

    @BeforeEach
    void setup() {
        pdndCacheableService = new PDNDCacheableService(
                pdndInfoCamereRestClient,
                tokenProviderPDND
        );
    }


    @Test
    void getEncryptedPDNDImpresa_success() {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder().build();

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

            String result = pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax, pdndSecretValue);

            assertThat(result).startsWith("ENCRYPTED:");
            verify(tokenProviderPDND).getTokenPdnd(pdndSecretValue);
            verify(pdndInfoCamereRestClient).retrieveInstitutionPdndByTaxCode(decryptedTax, "Bearer " + token);
        }
    }

    @Test
    void getEncryptedPDNDImpresa_returnsLastElementOfList() {
        // given
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder().build();

        when(tokenProviderPDND.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(token);

        PDNDImpresa olderImpresa = buildImpresa("OLD-CF");
        PDNDImpresa latestImpresa = buildImpresa("LATEST-CF");
        List<PDNDImpresa> list = new ArrayList<>(List.of(olderImpresa, latestImpresa));
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(decryptedTax, "Bearer " + token))
                .thenReturn(list);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            utils.when(() -> DataEncryptionUtils.encrypt(anyString()))
                    .thenAnswer(inv -> "ENCRYPTED:" + inv.getArgument(0, String.class));

            // when
            String result = pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax, pdndSecretValue);

            // then
            assertThat(result).contains("LATEST-CF").doesNotContain("OLD-CF");
        }
    }

    @Test
    void getEncryptedPDNDImpresa_emptyListThrowsResourceNotFound() {
        // given
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder().build();

        when(tokenProviderPDND.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(token);
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(decryptedTax, "Bearer " + token))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);

            // when
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax, pdndSecretValue));

            // then
            assertThat(ex).hasMessageContaining("No institution found for taxCode: " + decryptedTax);
            utils.verify(() -> DataEncryptionUtils.encrypt(anyString()), never());
        }
    }

    @Test
    void getEncryptedPDNDImpresa_exceptionWrappedAsIllegalArgument() {
        String encryptedTax = "ENC_TC";
        String decryptedTax = "TAXABC";
        String token = "tok-ic";

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        PdndSecretValue pdndSecretValue = PdndSecretValue.builder().build();

        when(tokenProviderPDND.getTokenPdnd(pdndSecretValue)).thenReturn(tokenResp);
        when(tokenResp.getAccessToken()).thenReturn(token);

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(encryptedTax)).thenReturn(decryptedTax);
            when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(anyString(), anyString()))
                    .thenThrow(new RuntimeException("boom"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pdndCacheableService.getEncryptedPDNDImpresa(encryptedTax, pdndSecretValue));
            assertThat(ex).hasMessageContaining("Unexpected error while retrieving institution");
        }
    }

    private PDNDImpresa buildImpresa(String taxCode) {
        PDNDImpresa impresa = new PDNDImpresa();
        impresa.setBusinessTaxId(taxCode);
        return impresa;
    }

}
