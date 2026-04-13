package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.service.ProductService;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductV2ControllerTest {

    @InjectMock
    ProductService productService;

    @Test
    void getOrigins() {
        String productId = "productId";
        when(productService.getOrigins(anyString()))
                .thenReturn(new OriginResult());

        given()
                .queryParam("productId", productId)
                .when()
                .get("/v2/product")
                .then()
                .statusCode(200);
    }

    @Test
    void getOrigins_missingProductId() {
        given()
                .when()
                .get("/v2/product")
                .then()
                .statusCode(400);
    }
}
