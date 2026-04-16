package it.pagopa.selfcare.mscore.connector.rest.mapper;

import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.AooResponse;
import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.UoResponse;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.UnitaOrganizzativa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UoMapper {

    UnitaOrganizzativa toEntity(UoResponse response);
}
