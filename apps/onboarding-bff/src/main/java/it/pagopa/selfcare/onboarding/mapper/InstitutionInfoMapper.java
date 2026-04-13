package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.client.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.onboarding.InstitutionUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "cdi")
public interface InstitutionInfoMapper {

    @Mapping(source = "city", target = "institutionLocation.city")
    @Mapping(source = "county", target = "institutionLocation.county")
    @Mapping(source = "country", target = "institutionLocation.country")
    InstitutionInfo toInstitutionInfo(Institution institution);


    Institution toInstitution(InstitutionUpdate institution);
}
