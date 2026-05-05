package it.pagopa.selfcare.external_api.utils;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionInfo;
import it.pagopa.selfcare.external_api.model.onboarding.ProductInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.function.BinaryOperator;

@ExtendWith({MockitoExtension.class})
class UserServiceUtilsTest {
    private OnboardedInstitutionInfo createInstitution(OffsetDateTime createdAt) {
        OnboardedInstitutionInfo institution = new OnboardedInstitutionInfo();
        institution.setProductInfo(new ProductInfo());
        institution.getProductInfo().setCreatedAt(createdAt);
        return institution;
    }

    @Test
    void testExistingIsLatest() {
        OnboardedInstitutionInfo existing = createInstitution(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        OnboardedInstitutionInfo replacement = createInstitution(OffsetDateTime.parse("2023-01-01T10:00:00Z"));
        BinaryOperator<OnboardedInstitutionInfo> comparator = UserServiceUtils.latestInstitutionByCreationDate();
        Assertions.assertEquals(existing, comparator.apply(existing, replacement));
    }

    @Test
    void testReplacementIsLatest() {
        OnboardedInstitutionInfo existing = createInstitution(OffsetDateTime.parse("2023-01-01T10:00:00Z"));
        OnboardedInstitutionInfo replacement = createInstitution(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        BinaryOperator<OnboardedInstitutionInfo> comparator = UserServiceUtils.latestInstitutionByCreationDate();
        Assertions.assertEquals(replacement, comparator.apply(existing, replacement));
    }

    @Test
    void testSameDateReturnsExisting() {
        OnboardedInstitutionInfo existing = createInstitution(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        OnboardedInstitutionInfo replacement = createInstitution(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        BinaryOperator<OnboardedInstitutionInfo> comparator = UserServiceUtils.latestInstitutionByCreationDate();
        Assertions.assertEquals(existing, comparator.apply(existing, replacement));
    }


    @Test
    void testNullValues() {
        OnboardedInstitutionInfo existing = createInstitution(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        BinaryOperator<OnboardedInstitutionInfo> comparator = UserServiceUtils.latestInstitutionByCreationDate();
        Assertions.assertEquals(existing, comparator.apply(existing, null));
        Assertions.assertEquals(existing, comparator.apply(null, existing));
        Assertions.assertThrows(NullPointerException.class, () -> comparator.apply(null, null));
    }
}
