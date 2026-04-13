package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.model.OnboardingProductDto;
import it.pagopa.selfcare.onboarding.model.CompanyOnboardingDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;

@QuarkusTest
class InstitutionControllerTest {

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
                .post("/v1/institutions/onboarding")
                .then()
                .statusCode(201);

        Mockito.verify(institutionService, Mockito.times(1))
                .onboardingProduct(any());
    }

    @Test
    void onboardingCompany() {
        CompanyOnboardingDto request = new CompanyOnboardingDto();
        request.setTaxCode("taxCode");
        request.setProductId("productId");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/institutions/company/onboarding")
                .then()
                .statusCode(201);

        Mockito.verify(institutionService, Mockito.times(1))
                .onboardingProduct(any());
    }

    @Test
    void getInstitutionOnboardingInfoById() {
        String institutionId = "institutionId";
        String productId = "productId";
        InstitutionOnboardingData data = new InstitutionOnboardingData();
        InstitutionInfo info = new InstitutionInfo();
        info.setId(institutionId);
        data.setInstitution(info);

        when(institutionService.getInstitutionOnboardingDataById(institutionId, productId))
                .thenReturn(data);

        given()
                .queryParam("institutionId", institutionId)
                .queryParam("productId", productId)
                .when()
                .get("/v1/institutions/onboarding/")
                .then()
                .statusCode(200)
                .body("institution.id", equalTo(institutionId));
    }

    @Test
    void getInstitutionGeographicTaxonomy() {
        String externalInstitutionId = "externalInstitutionId";
        GeographicTaxonomy geo = new GeographicTaxonomy();
        geo.setCode("code");
        geo.setDesc("desc");

        when(institutionService.getGeographicTaxonomyList(externalInstitutionId))
                .thenReturn(List.of(geo));

        given()
                .pathParam("externalInstitutionId", externalInstitutionId)
                .when()
                .get("/v1/institutions/{externalInstitutionId}/geographic-taxonomy")
                .then()
                .statusCode(200)
                .body("[0].code", equalTo("code"));
    }

    @Test
    @TestSecurity(user = "user", roles = {"user"})
    void getInstitutions() {
        String productId = "productId";
        InstitutionInfo info = new InstitutionInfo();
        info.setId("id");
        info.setExternalId("externalId");

        // Note: Principal mocking in QuarkusTest with RestAssured might need custom SecurityIdentity
        // if PrincipalUtils expects a specific type. For now, testing basic wiring.

        when(institutionService.getInstitutions(any(), any()))
                .thenReturn(List.of(info));

        given()
                .queryParam("productId", productId)
                .when()
                .get("/v1/institutions")
                .then()
                .statusCode(200)
                .body("[0].id", equalTo("id"));
    }

    @Test
    void verifyOnboarding() {
        String externalInstitutionId = "externalInstitutionId";
        String productId = "productId";

        given()
                .pathParam("externalInstitutionId", externalInstitutionId)
                .pathParam("productId", productId)
                .when()
                .head("/v1/institutions/{externalInstitutionId}/products/{productId}")
                .then()
                .statusCode(204);

        Mockito.verify(institutionService).verifyOnboarding(externalInstitutionId, productId);
    }
}
