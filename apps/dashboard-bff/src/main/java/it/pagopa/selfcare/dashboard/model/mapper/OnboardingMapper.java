package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.onboarding.OnboardingContractUpdateDto;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingContractUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(target = "productId", source = "productId")
    OnboardingContractUpdateRequest toOnboardingContractUpdateRequest(String productId, OnboardingContractUpdateDto onboardingContractUpdateDto);

}
