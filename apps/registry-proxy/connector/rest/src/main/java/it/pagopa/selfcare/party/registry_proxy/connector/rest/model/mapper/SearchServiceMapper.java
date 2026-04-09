package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchServiceMapper {

    SearchServiceOnboardingIndex toSearchServiceOnboardingIndex(OnboardingIndex onboardingIndex);

}
