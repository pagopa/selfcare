package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    OnboardingIndex toOnboardingIndex(OnboardingIndexResource onboardingIndexResource);

}
