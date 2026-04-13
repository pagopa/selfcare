package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.Institution;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.controller.response.BusinessResourceIC;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResource;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResourceIC;
import it.pagopa.selfcare.onboarding.controller.response.MatchInfoResultResource;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "jakarta-cdi")
public interface InstitutionResourceMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "userRole", source = "userRole.selfCareAuthority")
    InstitutionResource toResource(InstitutionInfo model);

    InstitutionResourceIC toResource(InstitutionInfoIC model);

    BusinessResourceIC toResource(BusinessInfoIC model);

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "institutionType", source = "institutionType", qualifiedByName = "enumToString")
    InstitutionResource toResource(Institution model);

    MatchInfoResultResource toResource(MatchInfoResult model);

    @Named("stringToUuid")
    default UUID stringToUuid(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    @Named("enumToString")
    default String enumToString(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

}
