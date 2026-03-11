package it.pagopa.selfcare.onboarding.connector;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.connector.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.OrganizationUnit;
import it.pagopa.selfcare.onboarding.connector.rest.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.connector.rest.model.AooResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.UoResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
class PartyRegistryProxyConnectorImpl implements PartyRegistryProxyConnector {

    protected static final String REQUIRED_FISCAL_CODE_MESSAGE = "An user's fiscal code is required";
    private static final String REQUIRED_EXTERNAL_ID_MESSAGE = "An institution's external id is required";

    private final PartyRegistryProxyRestClient restClient;

    private final RegistryProxyMapper proxyMapper;


    @Autowired
    public PartyRegistryProxyConnectorImpl(PartyRegistryProxyRestClient restClient, RegistryProxyMapper proxyMapper) {
        this.restClient = restClient;
        this.proxyMapper = proxyMapper;
    }


    @Override
    public InstitutionInfoIC getInstitutionsByUserFiscalCode(String taxCode) {
        log.trace("getInstitutionsByUserFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode taxCode = {}", taxCode);
        Assert.hasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        InstitutionByLegalTaxIdRequestDto institutionByLegalTaxIdRequestDto = new InstitutionByLegalTaxIdRequestDto();
        institutionByLegalTaxIdRequestDto.setLegalTaxId(taxCode);
        InstitutionByLegalTaxIdRequest institutionByLegalTaxIdRequest = new InstitutionByLegalTaxIdRequest();
        institutionByLegalTaxIdRequest.setFilter(institutionByLegalTaxIdRequestDto);
        InstitutionInfoIC result = restClient.getInstitutionsByUserLegalTaxId(institutionByLegalTaxIdRequest);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserFiscalCode result = {}", result);
        log.trace("getInstitutionsByUserFiscalCode end");
        return result;
    }


    @Override
    @Retry(name = "retryTimeout")
    public MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, String taxCode) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser taxCode = {}", taxCode);
        Assert.hasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        Assert.hasText(taxCode, REQUIRED_FISCAL_CODE_MESSAGE);
        MatchInfoResult result = restClient.matchInstitutionAndUser(externalInstitutionId, taxCode);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }

    @Override
    @Retry(name = "retryTimeout")
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", externalInstitutionId);
        Assert.hasText(externalInstitutionId, REQUIRED_EXTERNAL_ID_MESSAGE);
        InstitutionLegalAddressData result = restClient.getInstitutionLegalAddress(externalInstitutionId);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }

    @Override
    @Retry(name = "retryTimeout")
    public HomogeneousOrganizationalArea getAooById(String aooCode) {
        log.trace("getAooById start");
        log.debug("getAooById aooCode = {}", aooCode);
        AooResponse aooResponse = restClient.getAooById(aooCode);
        HomogeneousOrganizationalArea result = proxyMapper.toAOO(aooResponse);
        log.debug("getAooById result = {}", result);
        log.trace("getAooById end");
        return result;
    }

    @Override
    @Retry(name = "retryTimeout")
    public OrganizationUnit getUoById(String uoCode) {
        log.trace("getUoById start");
        log.debug("getUoById uoCode = {}", uoCode);
        UoResponse uoResponse = restClient.getUoById(uoCode);
        OrganizationUnit result = proxyMapper.toUO(uoResponse);
        log.debug("getUoById result = {}", result);
        log.trace("getUoById end");
        return result;
    }

    @Override
    @Retry(name = "retryTimeout")
    public GeographicTaxonomies getExtById(String code){
        log.trace("getExtById start");
        log.debug("getExtById code = {}", code);
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = restClient.getExtByCode(code);
        GeographicTaxonomies result = proxyMapper.toGeographicTaxonomies(geographicTaxonomiesResponse);
        log.debug("getExtById result = {}", result);
        log.trace("getExtById end");
        return result;
    }

    @Override
    @Retry(name = "retryTimeout")
    public InstitutionProxyInfo getInstitutionProxyById(String externalId) {
        log.trace("getInstitutionProxyById start");
        log.debug("getInstitutionProxyById externalId = {}", externalId);
        ProxyInstitutionResponse proxyInstitutionResponse = restClient.getInstitutionById(externalId);
        InstitutionProxyInfo institutionProxyInfo = proxyMapper.toInstitutionProxyInfo(proxyInstitutionResponse);
        log.debug("getInstitutionProxyById result = {}", institutionProxyInfo);
        log.trace("getInstitutionProxyById end");
        return institutionProxyInfo;
    }

}
