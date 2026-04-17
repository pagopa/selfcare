package it.pagopa.selfcare.onboarding.event.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.onboarding_functions_json.model.Onboarding;
import org.openapi.quarkus.party_registry_proxy_json.model.OnboardingIndexResource;

import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface OnboardingMapper {
    Onboarding toEntity(it.pagopa.selfcare.onboarding.event.entity.Onboarding model);

    @Mapping(target = "onboardingId", source = "id")
    @Mapping(target = "institutionId", source = "institution.id")
    @Mapping(target = "description", source = "institution.description")
    @Mapping(target = "parentDescription", source = "institution.parentDescription")
    @Mapping(target = "taxCode", source = "institution.taxCode")
    @Mapping(target = "subunitCode", source = "institution.subunitCode")
    @Mapping(target = "subunitType", source = "institution.subunitType")
    @Mapping(target = "institutionType", source = "institution.institutionType")
    OnboardingIndexResource toIndexResource(it.pagopa.selfcare.onboarding.event.entity.Onboarding model);
}
