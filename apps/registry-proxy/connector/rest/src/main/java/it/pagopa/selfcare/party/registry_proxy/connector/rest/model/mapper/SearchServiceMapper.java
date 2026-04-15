package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SearchServiceMapper {

    @Mapping(target = "action", constant = "mergeOrUpload")
    SearchServiceOnboardingIndex toSearchServiceOnboardingIndex(OnboardingIndex onboardingIndex);

    @Mapping(target = "totalElements", source = "count")
    @Mapping(target = "onboardings", source = "value")
    OnboardingIndexSearch toOnboardingIndexSearch(SearchServiceIndexResponse<SearchServiceOnboardingIndex> searchServiceIndexResponse);

    OnboardingIndex toOnboardingIndex(SearchServiceOnboardingIndex searchServiceOnboardingIndex);

}
