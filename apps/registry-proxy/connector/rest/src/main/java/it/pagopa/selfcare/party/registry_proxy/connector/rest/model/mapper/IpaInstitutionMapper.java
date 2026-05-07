package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.IpaInstitutionSearchResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.IpaInstitutionIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IpaInstitutionMapper
    extends IndexEntityMapper<IpaInstitution, IpaInstitutionIndex> {

  @Override
  @Mapping(target = "origin", source = "origin", qualifiedByName = "stringToOrigin")
  IpaInstitution toEntity(IpaInstitutionIndex ipaInstitutionIndex);

  @Override
  @Mapping(target = "totalElements", source = "count")
  @Mapping(target = "institutions", source = "value")
  IpaInstitutionSearchResult toSearchResult(
      SearchServiceIndexResponse<IpaInstitutionIndex> searchServiceIndexResponse);

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
