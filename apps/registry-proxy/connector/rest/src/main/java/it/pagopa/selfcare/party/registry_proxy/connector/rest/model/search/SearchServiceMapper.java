package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchServiceMapper {

    SearchServiceOnboardingIndex toSearchServiceOnboardingIndex(OnboardingIndex onboardingIndex);

}
