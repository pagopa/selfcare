package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequestDto;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResponse;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import it.pagopa.selfcare.onboarding.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.util.Preconditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

/**
 * Party registry proxy operations. Like the previous implementation every lookup but the info-camere one
 * is retried on connection problems only.
 */
@ApplicationScoped
@Slf4j
public class PartyRegistryProxyService {

    protected static final String REQUIRED_FISCAL_CODE_MESSAGE = "An user's fiscal code is required";
    private static final String REQUIRED_EXTERNAL_ID_MESSAGE = "An institution's external id is required";

    private final PartyRegistryProxyRestClient restClient;
    private final RegistryProxyMapper proxyMapper;

    public PartyRegistryProxyService(@RestClient PartyRegistryProxyRestClient restClient,
                                     RegistryProxyMapper proxyMapper) {
        this.restClient = restClient;
        this.proxyMapper = proxyMapper;
    }

    public InstitutionInfoIC getInstitutionsByUserFiscalCode(String taxCode) {
        log.trace("getInstitutionsByUserFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode taxCode = {}", taxCode);
        Preconditions.hasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);

        InstitutionByLegalTaxIdRequestDto filter = new InstitutionByLegalTaxIdRequestDto();
        filter.setLegalTaxId(taxCode);
        InstitutionByLegalTaxIdRequest request = new InstitutionByLegalTaxIdRequest();
        request.setFilter(filter);

        InstitutionInfoIC result = restClient.getInstitutionsByUserLegalTaxId(request);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode result = {}", result);
        log.trace("getInstitutionsByUserFiscalCode end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, String taxCode) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser taxCode = {}", taxCode);
        Preconditions.hasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        Preconditions.hasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        MatchInfoResult result = restClient.matchInstitutionAndUser(externalInstitutionId, taxCode);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", LogUtils.sanitize(externalInstitutionId));
        Preconditions.hasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        InstitutionLegalAddressData result = restClient.getInstitutionLegalAddress(externalInstitutionId);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public AooResponse getAooById(String aooCode) {
        log.trace("getAooById start");
        log.debug("getAooById aooCode = {}", LogUtils.sanitize(aooCode));
        AooResponse result = restClient.getAooById(aooCode);
        log.debug("getAooById result = {}", result);
        log.trace("getAooById end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public UoResponse getUoById(String uoCode) {
        log.trace("getUoById start");
        log.debug("getUoById uoCode = {}", LogUtils.sanitize(uoCode));
        UoResponse result = restClient.getUoById(uoCode);
        log.debug("getUoById result = {}", result);
        log.trace("getUoById end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public GeographicTaxonomiesResponse getExtById(String code) {
        log.trace("getExtById start");
        log.debug("getExtById code = {}", LogUtils.sanitize(code));
        GeographicTaxonomiesResponse result = restClient.getExtByCode(code);
        log.debug("getExtById result = {}", result);
        log.trace("getExtById end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public ProxyInstitutionResponse getInstitutionProxyById(String externalId) {
        log.trace("getInstitutionProxyById start");
        log.debug("getInstitutionProxyById externalId = {}", LogUtils.sanitize(externalId));
        ProxyInstitutionResponse result = restClient.getInstitutionById(externalId);
        log.debug("getInstitutionProxyById result = {}", result);
        log.trace("getInstitutionProxyById end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public InstitutionProxyInfo findIpaInstitutionByTaxCode(String taxCode, String category) {
        log.trace("findIpaInstitutionByTaxCode start");
        log.debug("findIpaInstitutionByTaxCode taxCode = {}", LogUtils.sanitize(taxCode));
        Preconditions.hasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        ProxyInstitutionResponse response = restClient.findIpaInstitutionByTaxCode(taxCode, category);
        InstitutionProxyInfo result = proxyMapper.toInstitutionProxyInfo(response);
        log.debug("findIpaInstitutionByTaxCode result = {}", result);
        log.trace("findIpaInstitutionByTaxCode end");
        return result;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public IpaInstitutionsSearchResult searchIpaInstitutions(String search, String category, Integer page, Integer pageSize) {
        log.trace("searchIpaInstitutions start");
        IpaInstitutionsSearchResponse response = restClient.searchIpaInstitutions(search, category, page, pageSize);
        IpaInstitutionsSearchResult result = proxyMapper.toIpaInstitutionsSearchResult(response);
        log.debug("searchIpaInstitutions result count = {}", result.getCount());
        log.trace("searchIpaInstitutions end");
        return result;
    }
}
