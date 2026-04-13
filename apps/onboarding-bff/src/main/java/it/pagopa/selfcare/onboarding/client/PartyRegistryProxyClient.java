package it.pagopa.selfcare.onboarding.client;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.client.model.*;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class PartyRegistryProxyClient {

    protected static final String REQUIRED_FISCAL_CODE_MESSAGE = "An user's fiscal code is required";
    private static final String REQUIRED_EXTERNAL_ID_MESSAGE = "An institution's external id is required";

    private final PartyRegistryProxyRestClient restClient;

    public PartyRegistryProxyClient(@RestClient PartyRegistryProxyRestClient restClient) {
        this.restClient = restClient;
    }

    public InstitutionInfoIC getInstitutionsByUserFiscalCode(String taxCode) {
        log.trace("getInstitutionsByUserFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode taxCode = {}", taxCode);
        requireHasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        
        InstitutionByLegalTaxIdRequestDto filter = new InstitutionByLegalTaxIdRequestDto();
        filter.setLegalTaxId(taxCode);
        InstitutionByLegalTaxIdRequest request = new InstitutionByLegalTaxIdRequest();
        request.setFilter(filter);
        
        InstitutionInfoIC result = restClient.getInstitutionsByUserLegalTaxId(request);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode result = {}", result);
        log.trace("getInstitutionsByUserFiscalCode end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000)
    public MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, String taxCode) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser taxCode = {}", taxCode);
        requireHasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        requireHasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        return restClient.matchInstitutionAndUser(externalInstitutionId, taxCode);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", externalInstitutionId);
        requireHasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        return restClient.getInstitutionLegalAddress(externalInstitutionId);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public AooResponse getAooById(String aooCode) {
        log.trace("getAooById start");
        log.debug("getAooById aooCode = {}", aooCode);
        return restClient.getAooById(aooCode);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public UoResponse getUoById(String uoCode) {
        log.trace("getUoById start");
        log.debug("getUoById uoCode = {}", uoCode);
        return restClient.getUoById(uoCode);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public GeographicTaxonomiesResponse getExtById(String code){
        log.trace("getExtById start");
        log.debug("getExtById code = {}", code);
        return restClient.getExtByCode(code);
    }

    @Retry(maxRetries = 2, delay = 5000)
    public ProxyInstitutionResponse getInstitutionProxyById(String externalId) {
        log.trace("getInstitutionProxyById start");
        log.debug("getInstitutionProxyById externalId = {}", externalId);
        return restClient.getInstitutionById(externalId);
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
