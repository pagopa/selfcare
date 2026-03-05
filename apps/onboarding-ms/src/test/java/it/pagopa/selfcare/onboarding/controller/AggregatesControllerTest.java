package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.service.AggregatesService;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(AggregatesController.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestSecurity(user = "userJwt")
class AggregatesControllerTest {

    @InjectMock
    AggregatesService aggregatesService;

    @TestSecurity(user = "userJwt")
    @Test
    void verifyAggregatesCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-appio.csv");

        when(aggregatesService.validateAppIoAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-io")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validateAppIoAggregatesCsv(any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getAggregatesCsv() {
        final String onboardingId = "onboardingId";
        final String productId = "productId";
        RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
        when(aggregatesService.retrieveAggregatesCsv(onboardingId,productId))
                .thenReturn(Uni.createFrom().item(response.build()));

        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .get("csv/{onboardingId}/products/{productId}", onboardingId, productId)
                .then()
                .statusCode(200);


    }

    @TestSecurity(user = "userJwt")
    @Test
    void verifyAggregatesSendCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-send.csv");

        when(aggregatesService.validateSendAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-pn")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validateSendAggregatesCsv(any());
    }


    @TestSecurity(user = "userJwt")
    @Test
    void verifyAggregatesPagoPaCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-pagopa.csv");

        when(aggregatesService.validatePagoPaAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-pagopa")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validatePagoPaAggregatesCsv(any());
    }
}
