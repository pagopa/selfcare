package it.pagopa.selfcare.mscore.web.model.mapper;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.web.model.institution.BrokerResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerMapper {

    BrokerResponse toBroker(Institution institution);
    List<BrokerResponse> toBrokers(List<Institution> institutions);
}
