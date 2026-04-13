package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductControllerTest {

    @InjectMock
    ProductService productService;

    @Test
    void getProduct() {
        String productId = "productId";
        Product product = new Product();
        product.setId(productId);

        when(productService.getProduct(any(), any()))
                .thenReturn(product);

        given()
                .pathParam("id", productId)
                .when()
                .get("/v1/product/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(productId));
    }

    @Test
    void getProducts() {
        Product product = new Product();
        product.setId("id");

        when(productService.getProducts(anyBoolean()))
                .thenReturn(List.of(product));

        given()
                .when()
                .get("/v1/products")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo("id"));
    }

    @Test
    void getProductsAdmin() {
        Product product = new Product();
        product.setId("id");
        Map<String, ContractTemplate> userContractMappings = new HashMap<>();
        ContractTemplate userContract = new ContractTemplate();
        userContract.setContractTemplatePath("test");
        userContractMappings.put(Product.CONTRACT_TYPE_DEFAULT, userContract);
        product.setUserContractMappings(userContractMappings);

        when(productService.getProducts(anyBoolean()))
                .thenReturn(List.of(product));

        given()
                .when()
                .get("/v1/products/admin")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo("id"));
    }
}
