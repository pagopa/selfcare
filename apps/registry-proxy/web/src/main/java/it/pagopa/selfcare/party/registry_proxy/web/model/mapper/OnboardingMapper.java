package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexSearchResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

  @Mapping(target = "statusUpdatedAt", source = ".", qualifiedByName = "toStatusUpdatedAt")
  OnboardingIndex toOnboardingIndex(OnboardingIndexResource onboardingIndexResource);

    OnboardingIndexSearchResource toOnboardingIndexSearchResource(OnboardingIndexSearch onboardingIndexSearch);

    OnboardingIndexResource toOnboardingIndexResource(OnboardingIndex onboardingIndex);

    @Named("toStatusUpdatedAt")
    default OffsetDateTime toStatusUpdatedAt(OnboardingIndexResource onboardingIndexResource) {
      return switch (OnboardingStatus.valueOf(onboardingIndexResource.getStatus())) {
        case COMPLETED -> onboardingIndexResource.getActivatedAt();
        case DELETED -> onboardingIndexResource.getDeletedAt();
        default -> null;
      };
    }

}
