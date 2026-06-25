package it.pagopa.selfcare.external_api.model.onboarding.mapper;

import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.Onboarding;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionInfo;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionResource;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionResponse;
import it.pagopa.selfcare.external_api.model.user.OnboardedProductResponse;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import it.pagopa.selfcare.external_api.model.user.UserInstitution;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")

public interface OnboardingInstitutionMapper {
    OnboardedInstitutionResponse toOnboardedInstitutionResponse(OnboardedInstitutionInfo onboardedInstitutionInfo);

    @Mapping(target = "userProductRoles", expression = "java(retrieveUserProductRole(userInstitution, productId))")
    @Mapping(target = "status", expression = "java(retrieveStatus(institution, productId))")
    @Mapping(target = "id", source = "institution.id")
    @Mapping(target = "institutionType", expression = "java(retrieveInstitutionType(institution))")
    OnboardedInstitutionResource toOnboardedInstitutionResource(Institution institution, UserInstitution userInstitution, String productId);


    @Named("retrieveStatus")
    default String retrieveStatus(Institution institution, String productId) {
        List<RelationshipState> statusList = institution.getOnboarding().stream()
                .filter(onboarding -> productId.equalsIgnoreCase(onboarding.getProductId()))
                .map(Onboarding::getStatus)
                .toList();

        if(CollectionUtils.isEmpty(statusList))
            return null;

        return Collections.min(statusList).name();
    }

    @Named("retrieveUserProductRole")
    default Collection<String> retrieveUserProductRole(UserInstitution userInstitution, String productId) {
        return userInstitution.getProducts().stream()
                .collect(Collectors.groupingBy(OnboardedProductResponse::getProductId, Collectors.mapping(OnboardedProductResponse::getProductRole, Collectors.toList())))
                .get(productId);
    }

    @Named("retrieveInstitutionType")
    default InstitutionType retrieveInstitutionType(Institution institution) {
        return Optional.ofNullable(institution)
                .map(Institution::getInstitutionType)
                .filter(type -> !it.pagopa.selfcare.onboarding.common.InstitutionType.GPU.name().equals(type))
                .map(type -> Enum.valueOf(InstitutionType.class, type))
                .orElse(null);
    }
}
