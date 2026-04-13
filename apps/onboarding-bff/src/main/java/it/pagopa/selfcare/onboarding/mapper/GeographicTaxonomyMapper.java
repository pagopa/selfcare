package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.controller.response.GeographicTaxonomyResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface GeographicTaxonomyMapper {
    GeographicTaxonomyResource toResource(GeographicTaxonomy model);

}
