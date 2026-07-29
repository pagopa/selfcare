package it.pagopa.selfcare.onboarding.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
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

