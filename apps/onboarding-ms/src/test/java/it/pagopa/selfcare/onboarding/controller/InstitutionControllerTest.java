package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.request.GetInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@TestHTTPEndpoint(InstitutionController.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class InstitutionControllerTest {

    @InjectMock InstitutionService institutionService;

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutions_withEmptyList() {

        given()
                .when()
                .body(new GetInstitutionRequest())
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutions() {
        final String institutionId = "institutionId";
        InstitutionResponse response = new InstitutionResponse();
        response.setId(institutionId);
        response.setInstitutionType(InstitutionType.GPU.name());

        GetInstitutionRequest request = new GetInstitutionRequest();
        request.setInstitutionIds(List.of(institutionId));

        Mockito.when(institutionService.getInstitutions(any()))
                .thenReturn(Multi.createFrom().item(response));

        given().when().body(request).contentType(ContentType.JSON).post().then().statusCode(200);
    }
}