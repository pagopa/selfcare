package it.pagopa.selfcare.onboarding.client;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.OrganizationUnit;
import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.client.rest.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.UoResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.client.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequestDto;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class PartyRegistryProxyConnectorImpl {

    protected static final String REQUIRED_FISCAL_CODE_MESSAGE = "An user's fiscal code is required";
    private static final String REQUIRED_EXTERNAL_ID_MESSAGE = "An institution's external id is required";

    private final PartyRegistryProxyRestClient restClient;

    private final RegistryProxyMapper proxyMapper;
    public PartyRegistryProxyConnectorImpl(@RestClient PartyRegistryProxyRestClient restClient, RegistryProxyMapper proxyMapper) {
        this.restClient = restClient;
        this.proxyMapper = proxyMapper;
    }
    public InstitutionInfoIC getInstitutionsByUserFiscalCode(String taxCode) {
        log.trace("getInstitutionsByUserFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode taxCode = {}", taxCode);
        requireHasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        InstitutionByLegalTaxIdRequestDto institutionByLegalTaxIdRequestDto = new InstitutionByLegalTaxIdRequestDto();
        institutionByLegalTaxIdRequestDto.setLegalTaxId(taxCode);
        InstitutionByLegalTaxIdRequest institutionByLegalTaxIdRequest = new InstitutionByLegalTaxIdRequest();
        institutionByLegalTaxIdRequest.setFilter(institutionByLegalTaxIdRequestDto);
        InstitutionInfoIC result = restClient.getInstitutionsByUserLegalTaxId(institutionByLegalTaxIdRequest);
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
        MatchInfoResult result = restClient.matchInstitutionAndUser(externalInstitutionId, taxCode);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", externalInstitutionId);
        requireHasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        InstitutionLegalAddressData result = restClient.getInstitutionLegalAddress(externalInstitutionId);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public HomogeneousOrganizationalArea getAooById(String aooCode) {
        log.trace("getAooById start");
        log.debug("getAooById aooCode = {}", aooCode);
        AooResponse aooResponse = restClient.getAooById(aooCode);
        HomogeneousOrganizationalArea result = proxyMapper.toAOO(aooResponse);
        log.debug("getAooById result = {}", result);
        log.trace("getAooById end");
        return result;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public OrganizationUnit getUoById(String uoCode) {
        log.trace("getUoById start");
        log.debug("getUoById uoCode = {}", uoCode);
        UoResponse uoResponse = restClient.getUoById(uoCode);
        OrganizationUnit result = proxyMapper.toUO(uoResponse);
        log.debug("getUoById result = {}", result);
        log.trace("getUoById end");
        return result;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public GeographicTaxonomies getExtById(String code){
        log.trace("getExtById start");
        log.debug("getExtById code = {}", code);
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = restClient.getExtByCode(code);
        GeographicTaxonomies result = proxyMapper.toGeographicTaxonomies(geographicTaxonomiesResponse);
        log.debug("getExtById result = {}", result);
        log.trace("getExtById end");
        return result;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public InstitutionProxyInfo getInstitutionProxyById(String externalId) {
        log.trace("getInstitutionProxyById start");
        log.debug("getInstitutionProxyById externalId = {}", externalId);
        ProxyInstitutionResponse proxyInstitutionResponse = restClient.getInstitutionById(externalId);
        InstitutionProxyInfo institutionProxyInfo = proxyMapper.toInstitutionProxyInfo(proxyInstitutionResponse);
        log.debug("getInstitutionProxyById result = {}", institutionProxyInfo);
        log.trace("getInstitutionProxyById end");
        return institutionProxyInfo;
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

}
