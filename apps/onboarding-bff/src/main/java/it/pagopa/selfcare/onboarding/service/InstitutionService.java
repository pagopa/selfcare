package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.*;
import java.util.List;

public interface InstitutionService {

    Uni<Void> onboardingProductV2(OnboardingData onboardingData);

    Uni<Void> onboardingCompanyV2(OnboardingData onboardingData, String userFiscalCode);

    Uni<Void> onboardingProduct(OnboardingData onboardingData);

    Uni<Void> onboardingPaAggregator(OnboardingData entity);

    Uni<List<InstitutionInfo>> getInstitutions(String productId, String userId);

    Uni<IpaInstitutionsSearchResult> searchIpaInstitutions(String search, String category, Integer page, Integer pageSize);

    Uni<InstitutionProxyInfo> findIpaInstitutionByTaxCode(String taxCode, String category);

    Uni<List<Institution>> getActiveOnboarding(String taxCode,String productId,String subunitCode);

    Uni<InstitutionOnboardingData> getInstitutionOnboardingDataById(String institutionId, String productId);

    Uni<InstitutionOnboardingData> getInstitutionOnboardingData(String externalInstitutionId, String productId);

    Uni<List<GeographicTaxonomy>> getGeographicTaxonomyList(String externalInstitutionId);

    Uni<Institution> getInstitutionByExternalId(String externalInstitutionId);

    Uni<List<GeographicTaxonomy>> getGeographicTaxonomyList(String taxCode, String subunitCode);

    Uni<Void> verifyOnboarding(String externalInstitutionId, String productId);

    Uni<Void> verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    Uni<Void> checkOrganization(String productId, String fiscalCode, String vatNumber);
    Uni<MatchInfoResult> matchInstitutionAndUser(String externalInstitutionId, User user);

    Uni<InstitutionLegalAddressData> getInstitutionLegalAddress(String externalInstitutionId);

    Uni<InstitutionInfoIC> getInstitutionsByUser(String taxCode);

    Uni<List<Institution>> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    Uni<VerifyAggregateResult> validateAggregatesCsv(UploadedFile file, String productId);

    Uni<RecipientCodeStatusResult> checkRecipientCode(String originId, String recipientCode);

    Uni<Void> onboardingUsersPgFromIcAndAde(OnboardingData onboardingUserPgRequest);

    Uni<ManagerVerification> verifyManager(String taxCode, String companyTaxCode);

    Uni<List<OnboardingResult>> getOnboardingWithFilter(String taxCode, String status);

    Uni<Void> validateOnboardingByProductOrInstitutionTaxCode(String taxCode, String productId);

    Uni<Void> triggerOnboardingRequest(String onboardingId);
}
