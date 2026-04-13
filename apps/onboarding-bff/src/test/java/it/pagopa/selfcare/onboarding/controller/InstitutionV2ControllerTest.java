package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.model.OnboardingProductDto;
import it.pagopa.selfcare.onboarding.model.CompanyOnboardingDto;
import it.pagopa.selfcare.onboarding.model.VerifyManagerRequest;
import it.pagopa.selfcare.onboarding.model.CompanyOnboardingUserDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
class InstitutionV2ControllerTest {

    @InjectMock
    InstitutionService institutionService;

    @Test
    void onboarding() {
        OnboardingProductDto request = new OnboardingProductDto();
        request.setTaxCode("taxCode");
        request.setProductId("productId");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v2/institutions/onboarding")
                .then()
                .statusCode(201);

        Mockito.verify(institutionService, Mockito.times(1))
                .onboardingProductV2(any());
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    void onboardingCompany() {
        CompanyOnboardingDto request = new CompanyOnboardingDto();
        request.setTaxCode("taxCode");
        request.setProductId("productId");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v2/institutions/company/onboarding")
                .then()
                .statusCode(201);

        Mockito.verify(institutionService, Mockito.times(1))
                .onboardingCompanyV2(any(), any());
    }

    @Test
    void getInstitution() {
        given()
                .queryParam("productId", "productId")
                .queryParam("origin", "origin")
                .queryParam("originId", "originId")
                .when()
                .get("/v2/institutions")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .getByFilters(anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void verifyAggregatesCsv() {
        given()
                .multiPart("aggregates", "hello.csv", "text/csv".getBytes())
                .formParam("productId", "prod-pagopa")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/v2/institutions/onboarding/aggregation/verification")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .validateAggregatesCsv(any(), anyString());
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    void verifyManager() {
        VerifyManagerRequest request = new VerifyManagerRequest();
        request.setCompanyTaxCode("taxCode");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v2/institutions/company/verify-manager")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .verifyManager(any(), anyString());
    }

    @Test
    void getActiveOnboarding() {
        given()
                .queryParam("taxCode", "taxCode")
                .queryParam("productId", "productId")
                .when()
                .get("/v2/institutions/onboarding/active")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .getActiveOnboarding(anyString(), anyString(), any());
    }

    @Test
    void checkRecipientCode() {
        given()
                .queryParam("originId", "originId")
                .queryParam("recipientCode", "recipientCode")
                .when()
                .get("/v2/institutions/onboarding/recipient-code/verification")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .checkRecipientCode(anyString(), anyString());
    }

    @Test
    void onboardingUsers() {
        CompanyOnboardingUserDto request = new CompanyOnboardingUserDto();
        request.setTaxCode("taxCode");
        request.setProductId("productId");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v2/institutions/onboarding/users/pg")
                .then()
                .statusCode(204);

        Mockito.verify(institutionService, Mockito.times(1))
                .onboardingUsersPgFromIcAndAde(any());
    }

    @Test
    void getOnboardingsInfo() {
        given()
                .queryParam("taxCode", "taxCode")
                .queryParam("status", "status")
                .when()
                .get("/v2/institutions/onboardings")
                .then()
                .statusCode(200);

        Mockito.verify(institutionService, Mockito.times(1))
                .getOnboardingWithFilter(anyString(), anyString());
    }
}
