package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.client.model.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.OrganizationUnit;
import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface RegistryProxyMapper {

    GeographicTaxonomies toGeographicTaxonomies(GeographicTaxonomiesResponse entity);

    InstitutionProxyInfo toInstitutionProxyInfo(ProxyInstitutionResponse entity);

    HomogeneousOrganizationalArea toAOO(AooResponse entity);

    OrganizationUnit toUO(UoResponse entity);

}
