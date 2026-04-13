package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.registry_proxy.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.registry_proxy.OrganizationUnit;
import it.pagopa.selfcare.onboarding.client.rest.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.rest.model.UoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface RegistryProxyMapper {

    GeographicTaxonomies toGeographicTaxonomies(GeographicTaxonomiesResponse entity);

    InstitutionProxyInfo toInstitutionProxyInfo(ProxyInstitutionResponse entity);

    HomogeneousOrganizationalArea toAOO(AooResponse entity);

    OrganizationUnit toUO(UoResponse entity);

}
