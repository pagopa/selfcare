package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndProfile;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Regression test that verifies the Redis cache key includes the PDND credential profile,
 * so results fetched with one subscription (e.g. SELFCARE) are not served to callers that
 * must use the other subscription (e.g. INVITALIA).
 * <p>
 * It runs with a real Spring caching proxy (so the {@code @Cacheable} SpEL key is evaluated)
 * backed by an in-memory cache manager registered under the {@code redisCacheManager} name.
 */
@SpringJUnitConfig(PDNDCacheableServiceCacheKeyTest.TestConfig.class)
class PDNDCacheableServiceCacheKeyTest {

    private static final String ENCRYPTED_TAX_CODE = "ENC_TC";
    private static final String DECRYPTED_TAX_CODE = "TAXABC";
    private static final String BEARER = "Bearer tok";

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean(name = "redisCacheManager")
        CacheManager redisCacheManager() {
            return new ConcurrentMapCacheManager("pdndInfocamere");
        }

        @Bean
        PDNDInfoCamereRestClient pdndInfoCamereRestClient() {
            return mock(PDNDInfoCamereRestClient.class);
        }

        @Bean
        TokenProvider tokenProviderPDND() {
            return mock(TokenProvider.class);
        }

        @Bean
        PDNDCacheableService pdndCacheableService(PDNDInfoCamereRestClient client, TokenProvider tokenProvider) {
            return new PDNDCacheableService(client, tokenProvider);
        }
    }

    @Autowired
    private PDNDCacheableService pdndCacheableService;
    @Autowired
    private PDNDInfoCamereRestClient pdndInfoCamereRestClient;
    @Autowired
    private TokenProvider tokenProviderPDND;
    @Autowired
    private CacheManager redisCacheManager;

    @BeforeEach
    void setup() {
        reset(pdndInfoCamereRestClient, tokenProviderPDND);
        Optional.ofNullable(redisCacheManager.getCache("pdndInfocamere")).ifPresent(Cache::clear);

        ClientCredentialsResponse tokenResp = mock(ClientCredentialsResponse.class);
        when(tokenResp.getAccessToken()).thenReturn("tok");
        when(tokenProviderPDND.getTokenPdnd(any())).thenReturn(tokenResp);

        List<PDNDImpresa> list = Collections.singletonList(new PDNDImpresa());
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(DECRYPTED_TAX_CODE, BEARER))
                .thenReturn(list);
    }

    @Test
    void sameProfileSameTaxCode_hitsCacheAndCallsUpstreamOnce() {
        PdndSecretValue selfcare = PdndSecretValue.builder().profile(PdndProfile.SELFCARE).build();

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(ENCRYPTED_TAX_CODE)).thenReturn(DECRYPTED_TAX_CODE);
            utils.when(() -> DataEncryptionUtils.encrypt(anyString()))
                    .thenAnswer(inv -> "ENCRYPTED:" + inv.getArgument(0, String.class));

            pdndCacheableService.getEncryptedPDNDImpresa(ENCRYPTED_TAX_CODE, selfcare);
            pdndCacheableService.getEncryptedPDNDImpresa(ENCRYPTED_TAX_CODE, selfcare);

            // second call must be served from cache -> only one upstream invocation
            verify(pdndInfoCamereRestClient, times(1))
                    .retrieveInstitutionPdndByTaxCode(DECRYPTED_TAX_CODE, BEARER);
        }
    }

    @Test
    void differentProfileSameTaxCode_doesNotShareCacheAndCallsUpstreamTwice() {
        PdndSecretValue selfcare = PdndSecretValue.builder().profile(PdndProfile.SELFCARE).build();
        PdndSecretValue invitalia = PdndSecretValue.builder().profile(PdndProfile.INVITALIA).build();

        try (MockedStatic<DataEncryptionUtils> utils = mockStatic(DataEncryptionUtils.class)) {
            utils.when(() -> DataEncryptionUtils.decrypt(ENCRYPTED_TAX_CODE)).thenReturn(DECRYPTED_TAX_CODE);
            utils.when(() -> DataEncryptionUtils.encrypt(anyString()))
                    .thenAnswer(inv -> "ENCRYPTED:" + inv.getArgument(0, String.class));

            pdndCacheableService.getEncryptedPDNDImpresa(ENCRYPTED_TAX_CODE, selfcare);
            pdndCacheableService.getEncryptedPDNDImpresa(ENCRYPTED_TAX_CODE, invitalia);

            // different profile -> different cache key -> two upstream invocations
            verify(pdndInfoCamereRestClient, times(2))
                    .retrieveInstitutionPdndByTaxCode(DECRYPTED_TAX_CODE, BEARER);
        }
    }
}

