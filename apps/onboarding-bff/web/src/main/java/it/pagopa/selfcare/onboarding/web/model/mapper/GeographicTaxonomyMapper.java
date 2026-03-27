package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.web.model.GeographicTaxonomyResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GeographicTaxonomyMapper {
    GeographicTaxonomyResource toResource(GeographicTaxonomy model);

}
