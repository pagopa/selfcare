package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

@QuarkusTest
class QueryUtilsTest {

    @Test
    void testOnboardingFilters() {

        final String taxCode = "taxCode";
        final Origin origin = Origin.IPA;
        final String originId = "originId";
        final String productId = "productId";
        final String subunitCode = "code";
        Institution institution = new Institution();
        institution.setTaxCode(taxCode);
        institution.setOrigin(origin);
        institution.setOriginId(originId);

        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setInstitution(institution);
        Map<String, String> onboardingMap = QueryUtils.createMapForInstitutionOnboardingsQueryParameter(taxCode, subunitCode, origin.name(), originId, OnboardingStatus.COMPLETED, productId);

        Assert.assertNotNull(onboardingMap);
        Assert.assertTrue(onboardingMap.get("institution.taxCode").equals(taxCode));
        Assert.assertTrue(onboardingMap.get("institution.origin").equals(origin.name()));
        Assert.assertTrue(onboardingMap.get("institution.originId").equals(originId));
        Assert.assertTrue(onboardingMap.get("institution.subunitCode").equals(subunitCode));
        Assert.assertTrue(onboardingMap.get("productId").equals(productId));

    }
}
