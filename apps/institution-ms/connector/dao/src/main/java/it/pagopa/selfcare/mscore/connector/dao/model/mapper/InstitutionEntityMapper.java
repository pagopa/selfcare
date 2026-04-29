package it.pagopa.selfcare.mscore.connector.dao.model.mapper;


import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface InstitutionEntityMapper {

    @Mapping(target = "id", defaultExpression = "java(UUID.randomUUID().toString())")
    InstitutionEntity convertToInstitutionEntity(Institution institution);

    Institution convertToInstitution(InstitutionEntity entity);
}
