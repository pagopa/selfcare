package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.Broker;
import it.pagopa.selfcare.backoffice.generated.openapi.v1.dto.BrokerPsp;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.BrokerResponse;
import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerMapper {

    @Mapping(source = "brokerCode", target = "code")
    BrokerInfo fromBrokerResource(Broker brokerResource);

    @Mapping(source = "id", target = "code")
    BrokerInfo fromInstitution(Institution institution);

    @Mapping(source = "taxCode", target = "code")
    BrokerInfo fromInstitution(BrokerResponse institutions);

    List<BrokerInfo> fromInstitutions(List<BrokerResponse> institutions);

    @Mapping(source = "brokerPspCode", target = "code")
    BrokerInfo fromBrokerPSPResource(BrokerPsp brokerPspResource);

}
