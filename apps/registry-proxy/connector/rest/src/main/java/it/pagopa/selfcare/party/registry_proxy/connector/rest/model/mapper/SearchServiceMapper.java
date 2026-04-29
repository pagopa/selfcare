package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.model.OnboardingIndexSearch;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.IpaInstitutionIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceOnboardingIndex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SearchServiceMapper {

    @Mapping(target = "action", constant = "mergeOrUpload")
    SearchServiceOnboardingIndex toSearchServiceOnboardingIndex(OnboardingIndex onboardingIndex);

    @Mapping(target = "totalElements", source = "count")
    @Mapping(target = "onboardings", source = "value")
    OnboardingIndexSearch toOnboardingIndexSearch(SearchServiceIndexResponse<SearchServiceOnboardingIndex> searchServiceIndexResponse);

    OnboardingIndex toOnboardingIndex(SearchServiceOnboardingIndex searchServiceOnboardingIndex);

    @Mapping(target = "origin", source = "origin", qualifiedByName = "stringToOrigin")
    @Mapping(target = "o", ignore = true)
    @Mapping(target = "ou", ignore = true)
    @Mapping(target = "aoo", ignore = true)
    IpaInstitution toIpaInstitution(IpaInstitutionIndex ipaInstitutionIndex);

    @Mapping(target = "totalElements", source = "count")
    @Mapping(target = "institutions", source = "value")
    IpaInstitutionSearchResult toIpaInstitutionSearchResult(SearchServiceIndexResponse<IpaInstitutionIndex> searchServiceIndexResponse);

    @Named("stringToOrigin")
    default Origin stringToOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        try {
            return Origin.fromValue(origin);
        } catch (Exception e) {
            return null;
        }
    }

}
