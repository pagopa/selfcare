package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.dashboard.model.user.OnboardingInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(target = "contractAvailable", expression = "java(isContractValid(onboardingResponse.getContract()))")
    @Mapping(target = "status", expression = "java(onboardingResponse.getStatus().name())")
    OnboardingInfo toOnboardingInfo(OnboardingResponse onboardingResponse);

    @Named("isContractValid")
    default Boolean isContractValid(String contract) {
        return Optional.ofNullable(contract)
                .filter(c -> !c.trim().isEmpty())
                .isPresent();
    }

}
