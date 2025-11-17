package it.pagopa.selfcare.product.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@QuarkusTest
@TestHTTPEndpoint(ProductController.class)
class ProductControllerTest {

    @InjectMock
    ProductService productService;

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup(){
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.DELEGATING));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new Jdk8Module());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void ping_shouldReturnOk() {
        // given
        when(productService.ping()).thenReturn(Uni.createFrom().item("pong"));

        // when
        given().accept(ContentType.JSON).when().get("/ping").then().statusCode(200).body(containsString("pong"));

        // then
        verify(productService, times(1)).ping();
    }


    @Test
    @TestSecurity(user = "userJwt")
    void createProduct_shouldReturnOK() {
        // given
        ProductCreateRequest productCreateRequest = getProductCreateRequest();

        ProductBaseResponse productBaseResponse = ProductBaseResponse.builder().productId("prod-test").status(ProductStatus.TESTING).id("prod-test-id").build();

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(Uni.createFrom().item(productBaseResponse));

        // when
        given().contentType(ContentType.JSON).body(productCreateRequest).when().post().then().statusCode(201).contentType(ContentType.JSON).body("id", equalTo("prod-test-id")).body("productId", equalTo("prod-test")).body("status", equalTo("TESTING"));

        // then
        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService, times(1)).createProduct(captor.capture());
        ProductCreateRequest passed = captor.getValue();
        Assertions.assertNotNull(captor);
        Assertions.assertEquals("prod-test", passed.getProductId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void createProduct_shouldReturnKo_whenBadRequest() {
        // given
        ProductCreateRequest productCreateRequest = new ProductCreateRequest();

        ProductBaseResponse productBaseResponse = new ProductBaseResponse();

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(Uni.createFrom().item(productBaseResponse));

        // when
        given().contentType(ContentType.JSON).body(productCreateRequest).when().post().then().statusCode(400).contentType(ContentType.JSON);

    }

    @Test
    @TestSecurity(user = "userJwt")
    void getProductById_shouldReturnOk() {
        // given
        ProductResponse response = getProductResponse();

        when(productService.getProductById("prod-test")).thenReturn(Uni.createFrom().item(response));

        // when
        given().accept(ContentType.JSON).when().get("/prod-test").then().statusCode(200).contentType(ContentType.JSON).body("productId", equalTo("prod-test")).body("status", equalTo("TESTING"));

        // then
        verify(productService, times(1)).getProductById("prod-test");
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getProductById_whenNotFound_shouldReturnKO() {
        // given
        String missing = "prod-ko";
        when(productService.getProductById(missing)).thenReturn(Uni.createFrom().failure(new NotFoundException("not found")));

        // when
        given().accept(ContentType.JSON).when().get("/" + missing).then().statusCode(404).contentType(ContentType.JSON).body("title", equalTo("Product not found")).body("status", equalTo(404)).body("detail", containsString(missing)).body("instance", equalTo("/products/" + missing));

        // then
        verify(productService, times(1)).getProductById(missing);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteProductTest_shouldReturn200() {
        // given
        String productId = "prod-test";

        ProductBaseResponse productBaseResponse = new ProductBaseResponse();

        when(productService.deleteProductById(productId))
                .thenReturn(Uni.createFrom().item(productBaseResponse));

        // when
        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/{productId}", productId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(notNullValue());

        // then
        verify(productService, times(1)).deleteProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteProductTest_ShouldReturn400_whenBadRequest() {
        // given
        String productId = "prod-test";

        when(productService.deleteProductById(productId))
                .thenReturn(Uni.createFrom().failure(new IllegalArgumentException()));

        // when
        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/{productId}", productId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Invalid productId"))
                .body("detail", equalTo("productId is required and must be non-blank"))
                .body("status", equalTo(400))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).deleteProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteProductTest_shouldReturn404_whenNotFound() {
        // given
        String productId = "missing";

        when(productService.deleteProductById(productId))
                .thenReturn(Uni.createFrom().failure(new NotFoundException("Product missing not found")));

        // when
        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/{productId}", productId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Product not found"))
                .body("detail", equalTo("No product found with productId=" + productId))
                .body("status", equalTo(404))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).deleteProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void patchProductTest_shouldReturn200_whenOk() {
        // given
        String productId = "prod-test";
        var updated = mock(ProductResponse.class);
        when(productService.patchProductById(eq(productId), any()))
                .thenReturn(Uni.createFrom().item(updated));
        String patchDoc = "{\"status\":\"TESTING\"}";

        // when
        given()
                .contentType("application/merge-patch+json")
                .accept(ContentType.JSON)
                .body(patchDoc)
                .when()
                .patch("/{productId}", productId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        // then
        verify(productService, times(1)).patchProductById(eq(productId), any());
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void patchProductTest_shouldReturn400_whenInvalidProductId() {
        // given
        String productId = StringUtils.EMPTY;
        when(productService.patchProductById(eq(productId), any()))
                .thenReturn(Uni.createFrom().failure(new IllegalArgumentException()));
        String patchDoc = "{\"productId\":\"\"}";

        // when
        given()
                .contentType("application/merge-patch+json")
                .accept(ContentType.JSON)
                .body(patchDoc)
                .when()
                .patch("/{productId}", productId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Invalid productId"))
                .body("detail", equalTo("productId is required and must be non-blank"))
                .body("status", equalTo(400))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).patchProductById(eq(productId), any());
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void patchProductTest_shouldReturn404_whenProductNotFound() {
        // given
        String productId = "prod-test";
        when(productService.patchProductById(eq(productId), any()))
                .thenReturn(Uni.createFrom().failure(new NotFoundException()));
        String patchDoc = "{\"productId\":\"prod-test\"}";

        // when
        given()
                .contentType("application/merge-patch+json")
                .accept(ContentType.JSON)
                .body(patchDoc)
                .when()
                .patch("/{productId}", productId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Product not found"))
                .body("detail", equalTo("No product found with productId=" + productId))
                .body("status", equalTo(404))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).patchProductById(eq(productId), any());
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void patchProductTest_shouldReturn500_whenRuntimeError() {
        // given
        String productId = "prod-test";
        when(productService.patchProductById(eq(productId), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));
        String patchDoc = "{}";

        // when
        given()
                .contentType("application/merge-patch+json")
                .accept(ContentType.JSON)
                .body(patchDoc)
                .when()
                .patch("/{productId}", productId)
                .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Internal Server Error"))
                .body("status", equalTo(500))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).patchProductById(eq(productId), any());
        verifyNoMoreInteractions(productService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void patchProductTest_shouldReturn400_whenInvalidPatchDocument() {
        // given
        String productId = "prod-test";
        String invalidPayload = "{}";

        when(productService.patchProductById(eq(productId), any()))
                .thenReturn(Uni.createFrom().failure(new BadRequestException("Invalid merge patch document")));

        // when
        given()
                .contentType("application/merge-patch+json")
                .accept(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .patch("/{productId}", productId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Bad Request"))
                .body("detail", equalTo("Invalid patch payload or field constraints violated: Invalid merge patch document"))
                .body("status", equalTo(400))
                .body("instance", equalTo("/products/" + productId));

        // then
        verify(productService, times(1)).patchProductById(eq(productId), any());
        verifyNoMoreInteractions(productService);
    }

    // UTILS
    private ProductCreateRequest getProductCreateRequest() {
        ProductCreateRequest productCreateRequest = null;
        try {
            productCreateRequest =  objectMapper.readValue(getClass().getResource("/request/createRequest.json"), ProductCreateRequest.class);
        } catch (IOException e) {
            log.error("Error", e);
        }
        return productCreateRequest;
    }

    private ProductResponse getProductResponse() {
        ProductResponse productResponse = null;
        try {
            productResponse =  objectMapper.readValue(getClass().getResource("/request/createRequest.json"), ProductResponse.class);
        } catch (IOException e) {
            log.error("Error", e);
        }
        return productResponse;
    }

}
