package it.pagopa.selfcare.mscore.core.mapper;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.InstitutionProxyInfo;
import it.pagopa.selfcare.mscore.model.institution.NationalRegistriesProfessionalAddress;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "origin", ignore = true)
    Institution fromInstitutionProxyInfo(InstitutionProxyInfo proxyInfo);

    Institution fromProfessionalAddress(NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress);

    default InstitutionType getInstitutionType(Institution institution, String productId) {
        return findOnboardingByProductId(institution, productId)
                .map(Onboarding::getInstitutionType)
                .orElse(null);
    }

    private Optional<Onboarding> findOnboardingByProductId(Institution institution, String productId) {
        return Optional.ofNullable(productId)
                .flatMap(id -> Optional.ofNullable(institution.getOnboarding())
                        .flatMap(onboardings -> onboardings.stream()
                                .filter(ob -> id.equals(ob.getProductId()))
                                .findFirst()));
    }
}
