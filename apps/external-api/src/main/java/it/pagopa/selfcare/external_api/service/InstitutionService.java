package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.SearchMode;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitution;
import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.user.UserProductResponse;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface InstitutionService {


    List<ProductResource> getInstitutionUserProductsV2(String institutionId, String userId);

    Collection<UserProductResponse> getInstitutionProductUsersV2(String institutionId, String productId, String userId, Set<String> productRoles, String xSelfCareUid);

    List<GeographicTaxonomy> getGeographicTaxonomyList(String institutionId);

    Collection<Institution> getInstitutionsByGeoTaxonomies(Set<String> geoTaxIds, SearchMode searchMode);

    String addInstitution(CreatePnPgInstitution request);

    LegalVerification verifyLegal(String taxId, String vatNumber);
}
