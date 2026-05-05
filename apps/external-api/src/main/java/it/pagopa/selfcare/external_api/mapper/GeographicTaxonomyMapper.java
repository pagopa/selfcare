package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomyResource;
import it.pagopa.selfcare.external_api.model.onboarding.GeographicTaxonomyDto;

public class GeographicTaxonomyMapper {

    public static GeographicTaxonomyResource toResource(GeographicTaxonomy model) {
        GeographicTaxonomyResource resource = null;
        if (model != null) {
            resource = new GeographicTaxonomyResource();
            resource.setCode(model.getCode());
            resource.setDesc(model.getDesc());
        }
        return resource;
    }

    public static GeographicTaxonomy fromDto(GeographicTaxonomyDto resource) {
        GeographicTaxonomy model = null;
        if (resource != null) {
            model = new GeographicTaxonomy();
            model.setCode(resource.getCode());
            model.setDesc(resource.getDesc());
        }

        return model;
    }
}
