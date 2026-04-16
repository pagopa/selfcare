package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.web.model.StationResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(source = "anacEngaged", target = "anacEngaged", qualifiedByName = "convertToBoolean")
    @Mapping(source = "anacEnabled", target = "anacEnabled", qualifiedByName = "convertToBoolean")
    StationResource toResource(Station station);

    @Named("convertToBoolean")
    default boolean convertToBoolean(String value) {
        return Objects.nonNull(value) ? Boolean.parseBoolean(value.trim()) : false;
    }

}
