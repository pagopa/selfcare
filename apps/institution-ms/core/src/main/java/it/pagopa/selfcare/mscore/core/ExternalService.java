package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.model.institution.GeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;

import java.util.List;

public interface ExternalService {

    Institution getInstitutionByExternalId(String externalId);

    Institution retrieveInstitutionProduct(String externalId, String productId);

    List<GeographicTaxonomies> retrieveInstitutionGeoTaxonomiesByExternalId(String externalId);

    List<Onboarding> retrieveInstitutionProductsByExternalId(String externalId, List<RelationshipState> states);

    List<Institution> retrieveInstitutionByIds(List<String> ids);
}
