package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDVisuraInfoCamereRawRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PDNDInfoCamereRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PDNDVisuraInfoCamereRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class PDNDCacheableService {

    private final TokenProviderVisura tokenProviderVisura;
    private final TokenProvider tokenProviderPDND;
    private final PDNDVisuraInfoCamereRawRestClient pdndVisuraInfoCamereRawRestClient;
    private final PDNDVisuraInfoCamereRestClientConfig pdndVisuraInfoCamereRestClientConfig;
    private final PDNDInfoCamereRestClientConfig pdndInfoCamereRestClientConfig;
    private final PDNDInfoCamereRestClient pdndInfoCamereRestClient;

    private static final String BEARER = "Bearer ";

    public PDNDCacheableService(TokenProviderVisura tokenProviderVisura,
                                PDNDVisuraInfoCamereRawRestClient pdndVisuraInfoCamereRawRestClient,
                                PDNDVisuraInfoCamereRestClientConfig pdndVisuraInfoCamereRestClientConfig,
                                PDNDInfoCamereRestClient pdndInfoCamereRestClient,
                                TokenProvider tokenProviderPDND,
                                PDNDInfoCamereRestClientConfig pdndInfoCamereRestClientConfig) {
        this.tokenProviderVisura = tokenProviderVisura;
        this.pdndVisuraInfoCamereRawRestClient = pdndVisuraInfoCamereRawRestClient;
        this.pdndVisuraInfoCamereRestClientConfig = pdndVisuraInfoCamereRestClientConfig;
        this.pdndInfoCamereRestClient = pdndInfoCamereRestClient;
        this.tokenProviderPDND = tokenProviderPDND;
        this.pdndInfoCamereRestClientConfig = pdndInfoCamereRestClientConfig;
    }

    @Cacheable(cacheNames = "pdndInfocamere", cacheManager = "redisCacheManager", key = "'retrieveInstitutionDetail:' + #encryptedTaxCode")
    public String getEncryptedDocument(String encryptedTaxCode) {
        log.info("getEncryptedDocument for {} START", encryptedTaxCode);
        ClientCredentialsResponse tokenResponse = tokenProviderVisura.getTokenPdnd(pdndVisuraInfoCamereRestClientConfig.getPdndSecretValue());
        String bearer = BEARER + tokenResponse.getAccessToken();
        var taxCode = DataEncryptionUtils.decrypt(encryptedTaxCode);
        try {
            byte[] document = pdndVisuraInfoCamereRawRestClient.getRawInstitutionDetail(taxCode, bearer);
            log.info("getEncryptedDocument for {} END", encryptedTaxCode);
            return DataEncryptionUtils.encrypt(new String(document, StandardCharsets.UTF_8));
        } catch (FeignException e) {
            if (e instanceof FeignException.BadRequest) {
                throw new ResourceNotFoundException("No institution found for taxCode: " + taxCode);
            }
            log.error("FeignException occurred while retrieving institution detail", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception occurred while retrieving institution detail", e);
            throw new IllegalArgumentException("Unexpected error while retrieving institution detail", e);
        }
    }

    @Cacheable(cacheNames = "pdndInfocamere", cacheManager = "redisCacheManager", key = "'retrieveInstitutionPdndByTaxCode:' + #encryptedTaxCode")
    public String getEncryptedPDNDImpresa(String encryptedTaxCode) {
        log.info("getEncryptedPDNDImpresa for {} START", encryptedTaxCode);
        String taxCode = DataEncryptionUtils.decrypt(encryptedTaxCode);

        ClientCredentialsResponse tokenResponse = tokenProviderPDND.getTokenPdnd(pdndInfoCamereRestClientConfig.getPdndSecretValue());
        String bearer = BEARER + tokenResponse.getAccessToken();

        try {
            PDNDImpresa impresa = pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(taxCode, bearer).get(0);
            return DataEncryptionUtils.encrypt(new ObjectMapper().writeValueAsString(impresa));
        } catch (Exception e) {
            log.error("Unexpected exception occurred while retrieving institution", e);
            throw new IllegalArgumentException("Unexpected error while retrieving institution", e);
        }

    }

}