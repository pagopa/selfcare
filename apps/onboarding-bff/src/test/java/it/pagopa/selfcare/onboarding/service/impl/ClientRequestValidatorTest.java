package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.InstitutionBaseRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingDefaultRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingPaRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingUserRequest;
import org.openapi.quarkus.onboarding_json.model.Origin;
import org.openapi.quarkus.onboarding_json.model.UserRequest;

class ClientRequestValidatorTest {

    private static Locale defaultLocale;
    private static ClientRequestValidator validator;

    @BeforeAll
    static void createValidator() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        validator = new ClientRequestValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @AfterAll
    static void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void validated_returnsTheSameValidRequest() {
        OnboardingUserRequest request = usersRequest();

        assertSame(request, validator.validated("_onboardingUsers", "onboardingUserRequest", request));
    }

    @Test
    void validated_nullRequestIsLeftToTheDownstream() {
        assertNull(validator.validated("_onboardingUsers", "onboardingUserRequest", null));
    }

    @Test
    void validated_missingOriginAndOriginId() {
        OnboardingUserRequest request = usersRequest().origin(null).originId(null);

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboardingUsers", "onboardingUserRequest", request));

        assertEquals("_onboardingUsers.onboardingUserRequest.origin: must not be null, "
                + "_onboardingUsers.onboardingUserRequest.originId: must not be null", e.getMessage());
    }

    @Test
    void validated_missingOriginOnly() {
        OnboardingUserRequest request = usersRequest().origin(null);

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboardingUsersAggregator", "onboardingUserRequest", request));

        assertEquals("_onboardingUsersAggregator.onboardingUserRequest.origin: must not be null", e.getMessage());
    }

    @Test
    void validated_emptyStringsViolateTheMinimumSize() {
        OnboardingUserRequest request = usersRequest().productId("").origin("").originId("");

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboardingUsers", "onboardingUserRequest", request));

        assertEquals("_onboardingUsers.onboardingUserRequest.origin: size must be between 1 and 2147483647, "
                + "_onboardingUsers.onboardingUserRequest.originId: size must be between 1 and 2147483647, "
                + "_onboardingUsers.onboardingUserRequest.productId: size must be between 1 and 2147483647",
                e.getMessage());
    }

    @Test
    void validated_emptyUsersViolateTheMinimumSize() {
        OnboardingUserRequest request = usersRequest().users(List.of());

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboardingUsers", "onboardingUserRequest", request));

        assertEquals("_onboardingUsers.onboardingUserRequest.users: size must be between 1 and 2147483647",
                e.getMessage());
    }

    @Test
    void validated_requiredNestedObjectsAreChecked() {
        OnboardingDefaultRequest request = new OnboardingDefaultRequest().productId("prod-io");

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboarding", "onboardingDefaultRequest", request));

        assertEquals("_onboarding.onboardingDefaultRequest.institution: must not be null", e.getMessage());
    }

    @Test
    void validated_nestedPathsKeepTheirIndex() {
        OnboardingPaRequest request = new OnboardingPaRequest().productId("prod-io")
                .institution(new InstitutionBaseRequest()
                        .originId("origin-id").origin(Origin.IPA));

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_onboardingPa", "onboardingPaRequest", request));

        assertEquals("_onboardingPa.onboardingPaRequest.institution.digitalAddress: must not be null, "
                + "_onboardingPa.onboardingPaRequest.institution.institutionType: must not be null",
                e.getMessage());
    }

    @Test
    void validated_checkManagerWithUuidUserId() {
        CheckManagerRequest valid = new CheckManagerRequest().productId("prod-io").userId(UUID.randomUUID());
        CheckManagerRequest missingUser = new CheckManagerRequest().productId("prod-io");

        assertSame(valid, validator.validated("_checkManager", "checkManagerRequest", valid));
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> validator.validated("_checkManager", "checkManagerRequest", missingUser));
        assertEquals("_checkManager.checkManagerRequest.userId: must not be null", e.getMessage());
    }

    private static OnboardingUserRequest usersRequest() {
        return new OnboardingUserRequest()
                .productId("prod-io")
                .origin("IPA")
                .originId("origin-id")
                .users(List.of(new UserRequest()));
    }
}
