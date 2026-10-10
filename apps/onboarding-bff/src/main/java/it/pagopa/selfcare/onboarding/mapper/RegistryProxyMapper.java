package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequestDto;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResponse;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.model.dto.response.IpaInstitutionResource;
import it.pagopa.selfcare.onboarding.model.dto.response.IpaInstitutionsSearchResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface RegistryProxyMapper {

    default InstitutionByLegalTaxIdRequest toInstitutionByLegalTaxIdRequest(String taxCode) {
        InstitutionByLegalTaxIdRequestDto filter = new InstitutionByLegalTaxIdRequestDto();
        filter.setLegalTaxId(taxCode);
        InstitutionByLegalTaxIdRequest request = new InstitutionByLegalTaxIdRequest();
        request.setFilter(filter);
        return request;
    }

    InstitutionProxyInfo toInstitutionProxyInfo(ProxyInstitutionResponse entity);

    IpaInstitutionsSearchResult toIpaInstitutionsSearchResult(IpaInstitutionsSearchResponse entity);

    IpaInstitutionResource toResource(InstitutionProxyInfo model);

    IpaInstitutionsSearchResource toResource(IpaInstitutionsSearchResult model);
}
