package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.OrganizationUnit;

public interface PartyRegistryProxyConnector {

    InstitutionInfoIC getInstitutionsByUserFiscalCode(String taxCode);

    MatchInfoResult matchInstitutionAndUser(String matchInstitutionAndUser, String userTaxCode);

    InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId);

    HomogeneousOrganizationalArea getAooById(String aooCode);

    OrganizationUnit getUoById(String uoCode);

    GeographicTaxonomies getExtById(String code);

    InstitutionProxyInfo getInstitutionProxyById(String externalId);

}
