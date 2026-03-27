package it.pagopa.selfcare.onboarding.web.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.Billing;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionOnboarding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class InstitutionOnboardingResourceTest {
    /**
     * Methods under test:
     * <ul>
     *   <li>{@link InstitutionOnboardingResource#equals(Object)}
     *   <li>{@link InstitutionOnboardingResource#hashCode()}
     * </ul>
     */
    @Test
    void testEqualsAndHashCode_whenOtherIsEqual_thenReturnEqual() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId("42");
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        InstitutionOnboardingResource institutionOnboardingResource2 = new InstitutionOnboardingResource();
        institutionOnboardingResource2.setInstitutionId("42");
        institutionOnboardingResource2.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertEquals(institutionOnboardingResource, institutionOnboardingResource2);
        int expectedHashCodeResult = institutionOnboardingResource.hashCode();
        assertEquals(expectedHashCodeResult, institutionOnboardingResource2.hashCode());
    }

    /**
     * Methods under test:
     * <ul>
     *   <li>{@link InstitutionOnboardingResource#equals(Object)}
     *   <li>{@link InstitutionOnboardingResource#hashCode()}
     * </ul>
     */
    @Test
    void testEqualsAndHashCode_whenOtherIsEqual_thenReturnEqual2() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId(null);
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        InstitutionOnboardingResource institutionOnboardingResource2 = new InstitutionOnboardingResource();
        institutionOnboardingResource2.setInstitutionId(null);
        institutionOnboardingResource2.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertEquals(institutionOnboardingResource, institutionOnboardingResource2);
        int expectedHashCodeResult = institutionOnboardingResource.hashCode();
        assertEquals(expectedHashCodeResult, institutionOnboardingResource2.hashCode());
    }

    /**
     * Method under test: {@link InstitutionOnboardingResource#equals(Object)}
     */
    @Test
    void testEquals_whenOtherIsDifferent_thenReturnNotEqual() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId("Institution Id");
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        InstitutionOnboardingResource institutionOnboardingResource2 = new InstitutionOnboardingResource();
        institutionOnboardingResource2.setInstitutionId("42");
        institutionOnboardingResource2.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertNotEquals(institutionOnboardingResource, institutionOnboardingResource2);
    }

    /**
     * Method under test: {@link InstitutionOnboardingResource#equals(Object)}
     */
    @Test
    void testEquals_whenOtherIsDifferent_thenReturnNotEqual2() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId(null);
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        InstitutionOnboardingResource institutionOnboardingResource2 = new InstitutionOnboardingResource();
        institutionOnboardingResource2.setInstitutionId("42");
        institutionOnboardingResource2.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertNotEquals(institutionOnboardingResource, institutionOnboardingResource2);
    }

    /**
     * Method under test: {@link InstitutionOnboardingResource#equals(Object)}
     */
    @Test
    void testEquals_whenOtherIsDifferent_thenReturnNotEqual3() {
        // Arrange
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("42");
        billing.setTaxCodeInvoicing("42");
        billing.setVatNumber("42");

        InstitutionOnboarding institutionOnboarding = new InstitutionOnboarding();
        institutionOnboarding.setBilling(billing);
        institutionOnboarding.setClosedAt(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        institutionOnboarding.setContract("42");
        institutionOnboarding.setCreatedAt(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        institutionOnboarding.setPricingPlan("42");
        institutionOnboarding.setProductId("42");
        institutionOnboarding.setStatus("42");
        institutionOnboarding.setTokenId("42");
        institutionOnboarding.setUpdatedAt(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));

        ArrayList<InstitutionOnboarding> onboardings = new ArrayList<>();
        onboardings.add(institutionOnboarding);

        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId("42");
        institutionOnboardingResource.setOnboardings(onboardings);

        InstitutionOnboardingResource institutionOnboardingResource2 = new InstitutionOnboardingResource();
        institutionOnboardingResource2.setInstitutionId("42");
        institutionOnboardingResource2.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertNotEquals(institutionOnboardingResource, institutionOnboardingResource2);
    }

    /**
     * Method under test: {@link InstitutionOnboardingResource#equals(Object)}
     */
    @Test
    void testEquals_whenOtherIsNull_thenReturnNotEqual() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId("42");
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertNotEquals(institutionOnboardingResource, null);
    }

    /**
     * Method under test: {@link InstitutionOnboardingResource#equals(Object)}
     */
    @Test
    void testEquals_whenOtherIsWrongType_thenReturnNotEqual() {
        // Arrange
        InstitutionOnboardingResource institutionOnboardingResource = new InstitutionOnboardingResource();
        institutionOnboardingResource.setInstitutionId("42");
        institutionOnboardingResource.setOnboardings(new ArrayList<>());

        // Act and Assert
        assertNotEquals(institutionOnboardingResource, "Different type to InstitutionOnboardingResource");
    }

    /**
     * Methods under test:
     * <ul>
     *   <li>default or parameterless constructor of
     * {@link InstitutionOnboardingResource}
     *   <li>{@link InstitutionOnboardingResource#setInstitutionId(String)}
     *   <li>{@link InstitutionOnboardingResource#setOnboardings(List)}
     *   <li>{@link InstitutionOnboardingResource#toString()}
     *   <li>{@link InstitutionOnboardingResource#getInstitutionId()}
     *   <li>{@link InstitutionOnboardingResource#getOnboardings()}
     * </ul>
     */
    @Test
    void testGettersAndSetters() {
        // Arrange and Act
        InstitutionOnboardingResource actualInstitutionOnboardingResource = new InstitutionOnboardingResource();
        actualInstitutionOnboardingResource.setInstitutionId("42");
        actualInstitutionOnboardingResource.setBusinessName("description");
        ArrayList<InstitutionOnboarding> onboardings = new ArrayList<>();
        actualInstitutionOnboardingResource.setOnboardings(onboardings);
        String actualToStringResult = actualInstitutionOnboardingResource.toString();
        String actualInstitutionId = actualInstitutionOnboardingResource.getInstitutionId();
        List<InstitutionOnboarding> actualOnboardings = actualInstitutionOnboardingResource.getOnboardings();

        // Assert that nothing has changed
        assertEquals("42", actualInstitutionId);
        assertEquals("InstitutionOnboardingResource(institutionId=42, businessName=description, onboardings=[])", actualToStringResult);
        assertTrue(actualOnboardings.isEmpty());
        assertSame(onboardings, actualOnboardings);
    }
}
