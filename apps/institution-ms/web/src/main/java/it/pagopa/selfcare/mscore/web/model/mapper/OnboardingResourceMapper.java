package it.pagopa.selfcare.mscore.web.model.mapper;


import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardingRequest;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardingUsersRequest;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionOnboardingRequest;
import it.pagopa.selfcare.mscore.web.model.institution.OnboardingPut;
import it.pagopa.selfcare.mscore.web.model.institution.OnboardingResponse;
import it.pagopa.selfcare.mscore.web.model.onboarding.OnboardingInstitutionRequest;
import it.pagopa.selfcare.mscore.web.model.onboarding.OnboardingInstitutionUsersRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = {InstitutionUpdateMapper.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface OnboardingResourceMapper {

    OnboardingResponse toResponse(Onboarding onboarding);

    @Mapping(target = "contractFilePath", source = "contractImported.filePath")
    @Mapping(target = "contractCreatedAt", source = "contractImported.createdAt")
    @Mapping(target = "billingRequest", source = "billing")
    @Mapping(target = "signContract", source = "signContract", qualifiedByName = "mapSignContract")
    @Mapping(target = "contractActivatedAt", source = "contractImported.activatedAt")
    OnboardingRequest toOnboardingRequest(OnboardingInstitutionRequest onboardingInstitutionRequest);

    OnboardingUsersRequest toOnboardingUsersRequest(OnboardingInstitutionUsersRequest request);

    @Named("mapSignContract")
    default Boolean mapSignContract(Boolean signContract) {
        return Optional.ofNullable(signContract).orElse(true);
    }

    @Mapping(target = "contract", source = "contractPath")
    @Mapping(target = "createdAt", source = "activatedAt")
    Onboarding toOnboarding(InstitutionOnboardingRequest onboardingRequest);

    @Named("toOffsetDateTime")
    default OffsetDateTime toOffsetDateTime(LocalDateTime date) {
        return Optional.ofNullable(date)
                .map(ld -> ld.atZone(ZoneOffset.systemDefault()).toOffsetDateTime())
                .orElse(null);
    }
    @Mapping(target = "contract", source = "contract.path")
    Onboarding toOnboarding(OnboardingRequest onboardingRequest);

    @Mapping(target = "billing.vatNumber", source = "vatNumber")
    Onboarding toOnboarding(OnboardingPut onboardingPut);
}
