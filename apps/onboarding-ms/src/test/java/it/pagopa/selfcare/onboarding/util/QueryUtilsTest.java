package it.pagopa.selfcare.onboarding.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static it.pagopa.selfcare.onboarding.common.Origin.MOCK;

@QuarkusTest
class QueryUtilsTest {

  @Test
  void testOnboardingUpdateMap_recipientCode() {
    Billing billing = new Billing();
    billing.setRecipientCode("recipientCode");

    Institution institution = new Institution();
    institution.setAddress("address");
    institution.setDescription("description");
    institution.setDigitalAddress("digitalAddress");
    institution.setCity("city");
    institution.setCountry("country");
    institution.setCounty("county");
    institution.setIstatCode("istatCode");
    institution.setZipCode("zipCode");
    institution.setRea("rea");
    institution.setShareCapital("shareCapital");
    institution.setBusinessRegisterPlace("businessRegisterPlace");
    institution.setSupportEmail("supportEmail");
    institution.setParentDescription("parentDescription");
    institution.setOrigin(MOCK);
    institution.setOriginId("MOCK1");

    Onboarding onboarding = new Onboarding();
    onboarding.setStatus(OnboardingStatus.COMPLETED);
    onboarding.setBilling(billing);
    onboarding.setInstitution(institution);
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setDeletedAt(LocalDateTime.now().plus(Duration.ofDays(1)));
    Map<String, Object> onboardingMap = QueryUtils.createMapForOnboardingUpdate(onboarding);

    Assert.assertNotNull(onboardingMap);
    Assert.assertTrue(onboardingMap.get("billing.recipientCode").equals("recipientCode"));
    Assert.assertNotNull(onboardingMap.get("status"));
    Assert.assertTrue(onboardingMap.get("status").equals(OnboardingStatus.COMPLETED.name()));
    Assert.assertNotNull(onboardingMap.get("createdAt"));
    Assert.assertTrue(onboardingMap.get("createdAt").equals(onboarding.getCreatedAt()));
    Assert.assertNotNull(onboardingMap.get("deletedAt"));
    Assert.assertTrue(onboardingMap.get("deletedAt").equals(onboarding.getDeletedAt()));

    Assert.assertTrue(onboardingMap.get("institution.address").equals("address"));
    Assert.assertTrue(onboardingMap.get("institution.description").equals("description"));
    Assert.assertTrue(onboardingMap.get("institution.digitalAddress").equals("digitalAddress"));
    Assert.assertTrue(onboardingMap.get("institution.city").equals("city"));
    Assert.assertTrue(onboardingMap.get("institution.country").equals("country"));
    Assert.assertTrue(onboardingMap.get("institution.county").equals("county"));
    Assert.assertTrue(onboardingMap.get("institution.zipCode").equals("zipCode"));
    Assert.assertTrue(onboardingMap.get("institution.istatCode").equals("istatCode"));
    Assert.assertTrue(onboardingMap.get("institution.rea").equals("rea"));
    Assert.assertTrue(onboardingMap.get("institution.shareCapital").equals("shareCapital"));
    Assert.assertTrue(onboardingMap.get("institution.businessRegisterPlace").equals("businessRegisterPlace"));
    Assert.assertTrue(onboardingMap.get("institution.supportEmail").equals("supportEmail"));
    Assert.assertTrue(onboardingMap.get("institution.parentDescription").equals("parentDescription"));
    Assert.assertTrue(onboardingMap.get("institution.origin").equals("MOCK"));
    Assert.assertTrue(onboardingMap.get("institution.originId").equals("MOCK1"));
  }
}
