package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.client.model.user.UserId;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingUserDto;
import it.pagopa.selfcare.onboarding.controller.request.CheckManagerDto;
import it.pagopa.selfcare.onboarding.controller.request.UserTaxCodeDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserControllerTest {

    @InjectMock
    UserService userService;

    @Test
    void validate() {
        UserDataValidationDto request = new UserDataValidationDto();
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/users/validate")
                .then()
                .statusCode(204);

        Mockito.verify(userService, Mockito.times(1))
                .validate(any());
    }

    @Test
    void onboarding() {
        OnboardingUserDto request = new OnboardingUserDto();
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/users/onboarding")
                .then()
                .statusCode(201);

        Mockito.verify(userService, Mockito.times(1))
                .onboardingUsers(any());
    }

    @Test
    void onboardingAggregator() {
        OnboardingUserDto request = new OnboardingUserDto();
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/users/onboarding/aggregator")
                .then()
                .statusCode(201);

        Mockito.verify(userService, Mockito.times(1))
                .onboardingUsersAggregator(any());
    }

    @Test
    void checkManager() {
        CheckManagerDto request = new CheckManagerDto();
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/users/check-manager")
                .then()
                .statusCode(200);

        Mockito.verify(userService, Mockito.times(1))
                .checkManager(any());
    }

    @Test
    void searchUser() {
        UserTaxCodeDto request = new UserTaxCodeDto();
        request.setTaxCode("taxCode");
        UserId userId = new UserId();
        userId.setId(UUID.randomUUID());

        when(userService.searchUser(any()))
                .thenReturn(userId);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/users/search-user")
                .then()
                .statusCode(200)
                .body("id", equalTo(userId.getId().toString()));
    }
}
