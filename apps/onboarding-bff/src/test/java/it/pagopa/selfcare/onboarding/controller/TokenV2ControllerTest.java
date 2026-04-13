package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.onboarding.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.client.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.model.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.service.UserInstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class TokenV2ControllerTest {

    @InjectMock
    TokenService tokenService;

    @InjectMock
    UserService userService;

    @InjectMock
    UserInstitutionService userInstitutionService;

    @Test
    void complete() {
        given()
                .multiPart("contract", "hello.pdf", "Hello world".getBytes())
                .pathParam("onboardingId", "42")
                .when()
                .post("/v2/tokens/{onboardingId}/complete")
                .then()
                .statusCode(204);

        Mockito.verify(tokenService, Mockito.times(1))
                .completeTokenV2(anyString(), any());
    }

    @Test
    void completeOnboardingUsers() {
        given()
                .multiPart("contract", "hello.pdf", "Hello world".getBytes())
                .pathParam("onboardingId", "42")
                .when()
                .post("/v2/tokens/{onboardingId}/complete-onboarding-users")
                .then()
                .statusCode(204);

        Mockito.verify(tokenService, Mockito.times(1))
                .completeOnboardingUsers(anyString(), any());
    }

    @Test
    void verifyOnboarding() {
        when(tokenService.verifyOnboarding(anyString()))
                .thenReturn(new OnboardingData());

        given()
                .pathParam("onboardingId", "42")
                .when()
                .post("/v2/tokens/{onboardingId}/verify")
                .then()
                .statusCode(200);
    }

    @Test
    void retrieveOnboardingRequest() {
        OnboardingData data = new OnboardingData();
        InstitutionUpdate iu = new InstitutionUpdate();
        iu.setTaxCode("taxCode");
        data.setInstitutionUpdate(iu);

        when(tokenService.getOnboardingWithUserInfo(anyString()))
                .thenReturn(data);

        given()
                .pathParam("onboardingId", "42")
                .when()
                .get("/v2/tokens/{onboardingId}")
                .then()
                .statusCode(200)
                .body("institutionInfo.fiscalCode", equalTo("taxCode"));
    }

    @Test
    void approveOnboarding() {
        given()
                .pathParam("onboardingId", "42")
                .when()
                .post("/v2/tokens/{onboardingId}/approve")
                .then()
                .statusCode(204);

        Mockito.verify(tokenService, Mockito.times(1))
                .approveOnboarding(anyString());
    }

    @Test
    void rejectOnboarding() {
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .pathParam("onboardingId", "42")
                .when()
                .post("/v2/tokens/{onboardingId}/reject")
                .then()
                .statusCode(204);

        Mockito.verify(tokenService, Mockito.times(1))
                .rejectOnboarding(anyString(), anyString());
    }

    @Test
    void deleteOnboarding() {
        given()
                .pathParam("onboardingId", "42")
                .when()
                .delete("/v2/tokens/{onboardingId}/complete")
                .then()
                .statusCode(204);

        Mockito.verify(tokenService, Mockito.times(1))
                .rejectOnboarding(anyString(), Mockito.eq("REJECTED_BY_USER"));
    }

    @Test
    void getContract() {
        BinaryData data = new BinaryData( "contract.pdf", "contract".getBytes());
        when(tokenService.getContract(anyString()))
                .thenReturn(data);

        given()
                .pathParam("onboardingId", "42")
                .when()
                .get("/v2/tokens/{onboardingId}/contract")
                .then()
                .statusCode(200)
                .contentType("application/pdf");
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    void getAggregatesCsv() {
        OnboardingData data = new OnboardingData();
        data.setStatus("COMPLETED");
        InstitutionUpdate iu = new InstitutionUpdate();
        iu.setId("instId");
        data.setInstitutionUpdate(iu);

        when(tokenService.getOnboardingWithUserInfo(anyString()))
                .thenReturn(data);
        when(userInstitutionService.verifyAllowedUserInstitution(anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(tokenService.getAggregatesCsv(anyString(), anyString()))
                .thenReturn(new BinaryData( "agg.csv", "csv".getBytes()));

        given()
                .pathParam("onboardingId", "42")
                .pathParam("productId", "prod")
                .when()
                .get("/v2/tokens/{onboardingId}/products/{productId}/aggregates-csv")
                .then()
                .statusCode(200);
    }
}
