package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import it.pagopa.selfcare.party.registry_proxy.web.model.UOResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UOMapper {

    UOResource toResource(UO uo);

}
