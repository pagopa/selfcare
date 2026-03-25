package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.web.model.InsuranceCompanyResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.StationResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InsuranceCompanyMapper {
    InsuranceCompanyResource toResource(InsuranceCompany company);

}
