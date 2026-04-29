package it.pagopa.selfcare.mscore.web.model.mapper;


import it.pagopa.selfcare.mscore.model.institution.InstitutionGeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface InstitutionUpdateMapper {

    @Mapping(target = "geographicTaxonomies", source = "geographicTaxonomyCodes", qualifiedByName = "toGeographicTaxonomies")
    InstitutionUpdate toInstitutionUpdate(InstitutionUpdateRequest request);

    @Named("toGeographicTaxonomies")
    default List<InstitutionGeographicTaxonomies> toGeographicTaxonomies(List<String> geographicTaxonomyCodes) {
        return Optional.ofNullable(geographicTaxonomyCodes)
                .map(item -> geographicTaxonomyCodes.stream()
                        .map(code -> new InstitutionGeographicTaxonomies(code,null))
                        .collect(Collectors.toList()))
                .orElse(null);
    }
}
