package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import java.io.IOException;
import jakarta.ws.rs.ProcessingException;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
@Slf4j
public class PartyRegistryProxyService {

    protected static final String REQUIRED_FISCAL_CODE_MESSAGE = "An user's fiscal code is required";
    private static final String REQUIRED_EXTERNAL_ID_MESSAGE = "An institution's external id is required";

    private final PartyRegistryProxyRestClient restClient;

    public PartyRegistryProxyService(@RestClient PartyRegistryProxyRestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
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

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, String taxCode) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser taxCode = {}", taxCode);
        requireHasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        requireHasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        MatchInfoResult result = restClient.matchInstitutionAndUser(externalInstitutionId, taxCode);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", LogUtils.sanitize(externalInstitutionId));
        requireHasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        InstitutionLegalAddressData result = restClient.getInstitutionLegalAddress(externalInstitutionId);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public AooResponse getAooById(String aooCode) {
        log.trace("getAooById start");
        log.debug("getAooById aooCode = {}", aooCode);
        AooResponse result = restClient.getAooById(aooCode);
        log.debug("getAooById result = {}", result);
        log.trace("getAooById end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public UoResponse getUoById(String uoCode) {
        log.trace("getUoById start");
        log.debug("getUoById uoCode = {}", uoCode);
        UoResponse result = restClient.getUoById(uoCode);
        log.debug("getUoById result = {}", result);
        log.trace("getUoById end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public GeographicTaxonomiesResponse getExtById(String code){
        log.trace("getExtById start");
        log.debug("getExtById code = {}", code);
        GeographicTaxonomiesResponse result = restClient.getExtByCode(code);
        log.debug("getExtById result = {}", result);
        log.trace("getExtById end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public ProxyInstitutionResponse getInstitutionProxyById(String externalId) {
        log.trace("getInstitutionProxyById start");
        log.debug("getInstitutionProxyById externalId = {}", externalId);
        ProxyInstitutionResponse result = restClient.getInstitutionById(externalId);
        log.debug("getInstitutionProxyById result = {}", result);
        log.trace("getInstitutionProxyById end");
        return result;
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
