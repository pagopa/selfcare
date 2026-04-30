package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.constant.SearchMode;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InstitutionConnector {

    Institution save(Institution example);

    List<Institution> findAll();

    void deleteById(String id);

    void findAndDeleteOnboarding(String institutionId, String productId);

    Boolean existsByTaxCodeAndSubunitCodeAndProductAndStatusList(String taxtCode, String subunitCode, Optional<String> productId, List<RelationshipState> validRelationshipStates);

    Optional<Institution> findByExternalId(String externalId);

    List<Institution> findWithFilter(String externalId, String productId, List<RelationshipState> validRelationshipStates);

    Institution findById(String id);

    Institution findByIdAndProduct(String id, String productId);

    Institution findAndUpdateStatus(String id, String tokenId, RelationshipState state);

    Institution findAndAddOnboarding(String id, Onboarding onboarding);

    Institution findAndUpdate(String id, List<InstitutionGeographicTaxonomies> geographicTaxonomies, InstitutionUpdate institutionUpdate);

    Institution findByExternalIdAndProductId(String externalId, String productId);

    List<Onboarding> findOnboardingByIdAndProductId(String externalId, String productId);

    List<Institution> findInstitutionsByProductId(String productId, Integer page, Integer size);

    void findAndRemoveOnboarding(String institutionId, Onboarding onboarding);

    List<Institution> findByGeotaxonomies(List<String> geo, SearchMode searchMode);

    List<Institution> findByProductId(String productId);

    List<Institution> findAllByIds(List<String> ids);

    List<String> findByExternalIdsAndProductId(List<ValidInstitution> externalIds, String productId);

    Institution updateOnboardedProductCreatedAt(String institutionId, String productId, OffsetDateTime createdAt);

    List<Institution> findBrokers(String productId, InstitutionType type);

    List<Institution> findByTaxCodeAndSubunitCode(String taxCode, String subunitCode, String productId);

    List<Institution> findByTaxCodeWithSubunits(String taxCode, String productId);

    List<Institution> findByOriginAndOriginId(String origin, String originId, String productId);

    Boolean existsOnboardingByFilters(VerifyOnboardingFilters filters);
}
