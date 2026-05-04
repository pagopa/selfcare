package it.pagopa.selfcare.mscore.connector.rest.mapper;

import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.AooResponse;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AooMapper {

    AreaOrganizzativaOmogenea toEntity(AooResponse response);
}
