package it.pagopa.selfcare.onboarding.core.mapper;

import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface InstitutionInfoMapper {

    @Mapping(source = "city", target = "institutionLocation.city")
    @Mapping(source = "county", target = "institutionLocation.county")
    @Mapping(source = "country", target = "institutionLocation.country")
    InstitutionInfo toInstitutionInfo(Institution institution);


    Institution toInstitution(InstitutionUpdate institution);
}
