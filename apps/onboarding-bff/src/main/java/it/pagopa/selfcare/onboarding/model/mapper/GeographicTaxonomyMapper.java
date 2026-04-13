package it.pagopa.selfcare.onboarding.model.mapper;

import it.pagopa.selfcare.onboarding.client.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.model.GeographicTaxonomyResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface GeographicTaxonomyMapper {
    GeographicTaxonomyResource toResource(GeographicTaxonomy model);

}
