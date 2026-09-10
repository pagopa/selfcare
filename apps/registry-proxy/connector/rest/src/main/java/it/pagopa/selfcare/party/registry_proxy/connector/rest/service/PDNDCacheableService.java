package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PDNDCacheableService {

    private final TokenProvider tokenProviderPDND;
    private final PDNDInfoCamereRestClient pdndInfoCamereRestClient;

    private static final String BEARER = "Bearer ";

    public PDNDCacheableService(PDNDInfoCamereRestClient pdndInfoCamereRestClient,
                                TokenProvider tokenProviderPDND) {
        this.pdndInfoCamereRestClient = pdndInfoCamereRestClient;
        this.tokenProviderPDND = tokenProviderPDND;
    }


    @Cacheable(cacheNames = "pdndInfocamere", cacheManager = "redisCacheManager", key = "'retrieveInstitutionPdndByTaxCode:' + #encryptedTaxCode")
    public String getEncryptedPDNDImpresa(String encryptedTaxCode, PdndSecretValue pdndSecretValue) {
        log.info("getEncryptedPDNDImpresa for {} START", encryptedTaxCode);
        String taxCode = DataEncryptionUtils.decrypt(encryptedTaxCode);

        ClientCredentialsResponse tokenResponse = tokenProviderPDND.getTokenPdnd(pdndSecretValue);
        String bearer = BEARER + tokenResponse.getAccessToken();

        try {
            List<PDNDImpresa> imprese = pdndInfoCamereRestClient.retrieveInstitutionPdndByTaxCode(taxCode, bearer);
            if (Objects.isNull(imprese) || imprese.isEmpty()) {
                throw new ResourceNotFoundException("No institution found for taxCode: " + taxCode);
            }
            int lastUpdatedIndex = imprese.size() - 1;
            log.info("InfoCamere returned {} records for taxCode {}, selecting last updated index {}", imprese.size(), taxCode, lastUpdatedIndex);
            PDNDImpresa impresa = imprese.get(lastUpdatedIndex);
            return DataEncryptionUtils.encrypt(new ObjectMapper().writeValueAsString(impresa));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception occurred while retrieving institution", e);
            throw new IllegalArgumentException("Unexpected error while retrieving institution", e);
        }

    }

}
