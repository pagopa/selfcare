package it.pagopa.selfcare.mscore.connector.rest.mapper;

import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.PdndResponse;
import it.pagopa.selfcare.mscore.model.institution.SaResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SaMapper {

    SaResource toResource(PdndResponse response);
}
