package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.entity.Institution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

@Mapper(componentModel = "cdi")
public interface InstitutionMapper {

    @Mapping(source = "rootParent.description", target = "parentDescription", nullValuePropertyMappingStrategy =  NullValuePropertyMappingStrategy.IGNORE)
    Institution toEntity(InstitutionResponse model);

    Institution toEntity(AggregateInstitutionRequest model);

}
