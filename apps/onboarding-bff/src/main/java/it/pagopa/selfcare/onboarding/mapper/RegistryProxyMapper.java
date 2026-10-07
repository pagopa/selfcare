package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResponse;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.controller.response.IpaInstitutionResource;
import it.pagopa.selfcare.onboarding.controller.response.IpaInstitutionsSearchResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface RegistryProxyMapper {

    InstitutionProxyInfo toInstitutionProxyInfo(ProxyInstitutionResponse entity);

    IpaInstitutionsSearchResult toIpaInstitutionsSearchResult(IpaInstitutionsSearchResponse entity);

    IpaInstitutionResource toResource(InstitutionProxyInfo model);

    IpaInstitutionsSearchResource toResource(IpaInstitutionsSearchResult model);
}
