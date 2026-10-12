package it.pagopa.selfcare.onboarding.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.util.ErrorMessage;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.Features;
import org.openapi.quarkus.product_json.model.ProductResponse;

@QuarkusTest
class OnboardingValidationHelperTest {

    private static final String PRODUCT_ID = "prod-io";

    @Inject
    OnboardingValidationHelper helper;

    @Test
    void verifySameUserManagerAndDelegate_flagTrue_skipsValidation() {
        // Even a colliding manager+delegate must NOT trigger any failure when the product
        // allows the same user in both roles.
        ProductResponse product = productWithFlag(true);
        List<UserRequest> users = List.of(
                user(PartyRole.MANAGER, "CF1", "same@example.com"),
                user(PartyRole.DELEGATE, "CF1", "same@example.com"));

        helper.verifySameUserManagerAndDelegate(users, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifySameUserManagerAndDelegate_noDuplicates_completesSuccessfully() {
        // Distinct taxCode AND distinct email → no duplicates → happy path.
        ProductResponse product = productWithFlag(false);
        List<UserRequest> users = List.of(
                user(PartyRole.MANAGER, "CF1", "manager@example.com"),
                user(PartyRole.DELEGATE, "CF2", "delegate@example.com"));

        helper.verifySameUserManagerAndDelegate(users, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifySameUserManagerAndDelegate_duplicateTaxCode_failsWithInvalidRequest() {
        // Same taxCode (email would trigger the same failure branch — one duplicate is enough).
        ProductResponse product = productWithFlag(false);
        List<UserRequest> users = List.of(
                user(PartyRole.MANAGER, "CF1", "manager@example.com"),
                user(PartyRole.DELEGATE, "CF1", "delegate@example.com"));

        Throwable failure = helper.verifySameUserManagerAndDelegate(users, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.MANAGER_AND_DELEGATE_SAME_USER.getCode(), ex.getCode());
    }

    // -------------------------------------------------------------------------
    // verifyRequiredRecipientCode
    // -------------------------------------------------------------------------

    @Test
    void verifyRequiredRecipientCode_notRequired_skipsValidation() {
        // Flag false → no check even if recipientCode is missing.
        ProductResponse product = productWithRequiredRecipientCode(false);
        Onboarding onboarding = onboardingWithRecipientCode(null);

        helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifyRequiredRecipientCode_nullFeatures_skipsValidation() {
        // No features at all → treated as not required → skipped.
        ProductResponse product = new ProductResponse();
        product.setProductId(PRODUCT_ID);
        Onboarding onboarding = onboardingWithRecipientCode(null);

        helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndPresent_completesSuccessfully() {
        // Flag true and recipientCode present → happy path.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode("REC123");

        helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndNull_failsWithInvalidRequest() {
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode(null);

        Throwable failure = helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.RECIPIENT_CODE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndBlank_failsWithInvalidRequest() {
        // Blank recipientCode is treated as missing.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode("   ");

        Throwable failure = helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.RECIPIENT_CODE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndNullBilling_failsWithInvalidRequest() {
        // No billing block at all → recipientCode considered missing.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = new Onboarding();

        Throwable failure = helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.RECIPIENT_CODE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndMissingForPT_skipsValidation() {
        // PT institutions never provide a recipient code → exempted even if the product requires it.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode(null, InstitutionType.PT);

        helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndNullBillingForPT_skipsValidation() {
        // PT without any billing block → still exempted.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institution(InstitutionType.PT));

        helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(null);
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndMissingForPA_failsWithInvalidRequest() {
        // Non-exempted institution type → the product flag is still enforced.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode(null, InstitutionType.PA);

        Throwable failure = helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.RECIPIENT_CODE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void verifyRequiredRecipientCode_requiredAndMissingWithNullInstitutionType_failsWithInvalidRequest() {
        // Institution present but without type → not exempted.
        ProductResponse product = productWithRequiredRecipientCode(true);
        Onboarding onboarding = onboardingWithRecipientCode(null, null);

        Throwable failure = helper.verifyRequiredRecipientCode(onboarding, product)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed()
                .getFailure();

        InvalidRequestException ex = assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals(ErrorMessage.RECIPIENT_CODE_REQUIRED.getCode(), ex.getCode());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ProductResponse productWithFlag(boolean allow) {
        Features features = new Features();
        features.setAllowSameUserManagerAndDelegate(allow);
        ProductResponse product = new ProductResponse();
        product.setProductId(PRODUCT_ID);
        product.setFeatures(features);
        return product;
    }

    private static ProductResponse productWithRequiredRecipientCode(boolean required) {
        Features features = new Features();
        features.setRequiredRecipientCode(required);
        ProductResponse product = new ProductResponse();
        product.setProductId(PRODUCT_ID);
        product.setFeatures(features);
        return product;
    }

    private static Onboarding onboardingWithRecipientCode(String recipientCode) {
        Billing billing = new Billing();
        billing.setRecipientCode(recipientCode);
        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        return onboarding;
    }

    private static Onboarding onboardingWithRecipientCode(String recipientCode, InstitutionType institutionType) {
        Onboarding onboarding = onboardingWithRecipientCode(recipientCode);
        onboarding.setInstitution(institution(institutionType));
        return onboarding;
    }

    private static Institution institution(InstitutionType institutionType) {
        Institution institution = new Institution();
        institution.setInstitutionType(institutionType);
        return institution;
    }

    private static UserRequest user(PartyRole role, String taxCode, String email) {
        return UserRequest.builder()
                .role(role)
                .taxCode(taxCode)
                .email(email)
                .name("name-" + taxCode)
                .surname("surname-" + taxCode)
                .build();
    }
}

