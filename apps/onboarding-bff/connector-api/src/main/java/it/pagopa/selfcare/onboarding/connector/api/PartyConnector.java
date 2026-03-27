package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.RelationshipsResponse;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.connector.model.institutions.OnboardingResource;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.UserInfo;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Collection;
import java.util.List;

public interface PartyConnector {

    void onboardingOrganization(OnboardingData onboardingData);

    List<InstitutionInfo> getInstitutionsByUser(Product product, String userId);

    RelationshipsResponse getUserInstitutionRelationships(String externalInstitutionId, UserInfo.UserInfoFilter userInfoFilter);

    Collection<UserInfo> getUsers(String externalInstitutionId, UserInfo.UserInfoFilter userInfoFilter);

    List<Institution> getInstitutionsByTaxCodeAndSubunitCode(String taxCode, String subunitCode);

    Institution getInstitutionByExternalId(String externalInstitutionId);

    Institution getInstitutionById(String institutionId);

    List<OnboardingResource> getOnboardings(String institutionId, String productId);

    Institution createInstitutionFromIpa(String taxCode, String subunitCode, String subunitType);

    Institution createInstitutionFromANAC(OnboardingData onboardingData);

    Institution createInstitutionFromIVASS(OnboardingData onboardingData);

    Institution createInstitutionUsingExternalId(String institutionExternalId);

    Institution createInstitutionFromInfocamere(OnboardingData onboardingData);

    Institution createInstitution(OnboardingData onboardingData);

    UserInfo getInstitutionManager(String externalInstitutionId, String productId);

    InstitutionInfo getInstitutionBillingData(String externalId, String productId);

    void verifyOnboarding(String externalInstitutionId, String productId);

    void verifyOnboarding(String productId, String externalId, String taxCode, String origin, String originId, String subunitCode);

}
