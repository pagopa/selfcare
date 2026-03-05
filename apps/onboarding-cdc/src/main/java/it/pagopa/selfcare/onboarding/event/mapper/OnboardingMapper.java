package it.pagopa.selfcare.onboarding.event.mapper;


import org.mapstruct.Mapper;
import org.openapi.quarkus.onboarding_functions_json.model.Onboarding;

import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface OnboardingMapper {
    Onboarding toEntity(it.pagopa.selfcare.onboarding.event.entity.Onboarding model);
}
