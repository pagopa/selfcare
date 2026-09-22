package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.web.model.IpaInstitutionResource;
import it.pagopa.selfcare.onboarding.web.model.IpaInstitutionsSearchResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IpaInstitutionsSearchResourceMapper {

    IpaInstitutionsSearchResource toResource(IpaInstitutionsSearchResult model);

    IpaInstitutionResource toResource(InstitutionProxyInfo model);
}
