package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.institutions.*;
import it.pagopa.selfcare.onboarding.client.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.onboarding.User;
import java.util.List;

public interface InstitutionService {

    void onboardingProductV2(OnboardingData onboardingData);

    void onboardingCompanyV2(OnboardingData onboardingData, String userFiscalCode);

    void onboardingProduct(OnboardingData onboardingData);

    void onboardingPaAggregator(OnboardingData entity);

    List<InstitutionInfo> getInstitutions(String productId, String userId);

    List<Institution> getActiveOnboarding(String taxCode,String productId,String subunitCode);

    InstitutionOnboardingData getInstitutionOnboardingDataById(String institutionId, String productId);

    InstitutionOnboardingData getInstitutionOnboardingData(String externalInstitutionId, String productId);

    List<GeographicTaxonomy> getGeographicTaxonomyList(String externalInstitutionId);

    Institution getInstitutionByExternalId(String externalInstitutionId);

    List<GeographicTaxonomy> getGeographicTaxonomyList(String taxCode, String subunitCode);

    void verifyOnboarding(String externalInstitutionId, String productId);

    void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType);

    void checkOrganization(String productId, String fiscalCode, String vatNumber);
    MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, User user);

    InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId);

    InstitutionInfoIC getInstitutionsByUser(String taxCode);

    List<Institution> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode);

    VerifyAggregateResult validateAggregatesCsv(UploadedFile file, String productId);

    RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode);

    void onboardingUsersPgFromIcAndAde(OnboardingData onboardingUserPgRequest);

    ManagerVerification verifyManager(String taxCode, String companyTaxCode);

    List<OnboardingResult> getOnboardingWithFilter(String taxCode, String status);

    void validateOnboardingByProductOrInstitutionTaxCode(String taxCode, String productId);
}
