package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomyList;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.UpdateInstitutionResource;
import it.pagopa.selfcare.dashboard.model.product.ProductTree;

import java.util.List;

public interface InstitutionService {

    Institution getInstitutionById(String institutionId);

    void updateInstitutionGeographicTaxonomy(String institutionId, GeographicTaxonomyList geographicTaxonomies);

    List<ProductTree> getProductsTree();

    Institution updateInstitutionDescription(String institutionId, UpdateInstitutionResource updatePnPGInstitutionResource);

    Institution findInstitutionById(String institutionId);
}
