package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.*;
import java.util.List;

public interface InstitutionService {

    Uni<Void> onboardingProductV2(OnboardingData onboardingData);

    void onboardingCompanyV2(OnboardingData onboardingData, String userFiscalCode);

    Uni<Void> onboardingProduct(OnboardingData onboardingData);

    Uni<Void> onboardingPaAggregator(OnboardingData entity);

    Uni<List<InstitutionInfo>> getInstitutions(String productId, String userId);

    IpaInstitutionsSearchResult searchIpaInstitutions(String search, String category, Integer page, Integer pageSize);

    InstitutionProxyInfo findIpaInstitutionByTaxCode(String taxCode, String category);

    List<Institution> getActiveOnboarding(String taxCode,String productId,String subunitCode);

    InstitutionOnboardingData getInstitutionOnboardingDataById(String institutionId, String productId);

    InstitutionOnboardingData getInstitutionOnboardingData(String externalInstitutionId, String productId);

    List<GeographicTaxonomy> getGeographicTaxonomyList(String externalInstitutionId);

    Institution getInstitutionByExternalId(String externalInstitutionId);

    List<GeographicTaxonomy> getGeographicTaxonomyList(String taxCode, String subunitCode);

    Uni<Void> verifyOnboarding(String externalInstitutionId, String productId);

    Uni<Void> verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    Uni<Void> checkOrganization(String productId, String fiscalCode, String vatNumber);
    MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, User user);

    InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId);

    Uni<InstitutionInfoIC> getInstitutionsByUser(String taxCode);

    List<Institution> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    VerifyAggregateResult validateAggregatesCsv(UploadedFile file, String productId);

    RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingUserPgRequest);

    ManagerVerification verifyManager(String taxCode, String companyTaxCode);

    List<OnboardingResult> getOnboardingWithFilter(String taxCode, String status);

    Uni<Void> validateOnboardingByProductOrInstitutionTaxCode(String taxCode, String productId);

    void triggerOnboardingRequest(String onboardingId);
}
