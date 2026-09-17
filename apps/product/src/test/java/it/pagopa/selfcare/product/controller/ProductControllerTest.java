package it.pagopa.selfcare.product.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductExpirationResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductRoleResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
import it.pagopa.selfcare.product.model.dto.response.WorkflowTypeResponse;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.model.enums.UserRole;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Slf4j
@QuarkusTest
@TestHTTPEndpoint(ProductController.class)
class ProductControllerTest {

  private static ObjectMapper objectMapper;
  @InjectMock ProductService productService;

  @BeforeAll
  static void setup() {
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
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/ping")
        .then()
        .statusCode(200)
        .body(containsString("pong"));

    // then
    verify(productService, times(1)).ping();
  }

  @Test
  @TestSecurity(user = "userJwt")
  void createProduct_shouldReturnOK() {
    // given
    ProductCreateRequest productCreateRequest = getProductCreateRequest();

    ProductBaseResponse productBaseResponse =
        ProductBaseResponse.builder()
            .productId("prod-test")
            .status(ProductStatus.TESTING)
            .id("prod-test-id")
            .build();

    when(productService.createProduct(any(ProductCreateRequest.class), anyString()))
        .thenReturn(Uni.createFrom().item(productBaseResponse));

    // when
    given()
        .queryParam("productId", "prod-test")
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .body(productCreateRequest)
        .when()
        .post()
        .then()
        .statusCode(201)
        .contentType(ContentType.JSON)
        .body("id", equalTo("prod-test-id"))
        .body("productId", equalTo("prod-test"))
        .body("status", equalTo("TESTING"));

    // then
    ArgumentCaptor<ProductCreateRequest> captor =
        ArgumentCaptor.forClass(ProductCreateRequest.class);
    verify(productService, times(1)).createProduct(captor.capture(), anyString());
    ProductCreateRequest passed = captor.getValue();
    Assertions.assertNotNull(captor);
    Assertions.assertEquals("prod-test", passed.getProductId());
    Assertions.assertEquals(10, passed.getRoleMappings().size());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void createProduct_shouldReturnKo_whenBadRequest() {
    // given
    ProductCreateRequest productCreateRequest = new ProductCreateRequest();

    ProductBaseResponse productBaseResponse = new ProductBaseResponse();

    when(productService.createProduct(any(ProductCreateRequest.class), anyString()))
        .thenReturn(Uni.createFrom().item(productBaseResponse));

    // when
    given()
        .queryParam("productId", "prod-test")
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .body(productCreateRequest)
        .when()
        .post()
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProductById_shouldReturnOk() {
    // given
    ProductResponse response = getProductResponse();

    when(productService.getProductById("prod-test")).thenReturn(Uni.createFrom().item(response));

    // when
    given()
        //                .pathParam("productId", "prod-test")
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .get("prod-test")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("productId", equalTo("prod-test"))
        .body("status", equalTo("TESTING"));

    // then
    verify(productService, times(1)).getProductById("prod-test");
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProductById_whenNotFound_shouldReturnKO() {
    // given
    String missing = "prod-ko";
    when(productService.getProductById(missing))
        .thenReturn(Uni.createFrom().failure(new NotFoundException("not found")));

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .get(missing)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", containsString(missing))
        .body("instance", equalTo("/products/" + missing));

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
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .delete(productId)
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
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .delete(productId)
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
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .delete(productId)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("detail", equalTo("No product found with productId: " + productId))
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
    ProductResponse updated = mock(ProductResponse.class);

    when(productService.patchProductById(
            eq(productId), eq("createdBy"), any(ProductPatchRequest.class)))
        .thenReturn(Uni.createFrom().item(updated));

    String patchDoc = "{\"status\":\"TESTING\"}";

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(patchDoc)
        .when()
        .patch(productId)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    // then
    verify(productService, times(1))
        .patchProductById(eq(productId), eq("createdBy"), any(ProductPatchRequest.class));
    verifyNoMoreInteractions(productService);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void patchProductTest_shouldReturn400_whenInvalidProductId() {
    // given
    String productId = " ";

    when(productService.patchProductById(
            eq(productId), eq("createdBy"), any(ProductPatchRequest.class)))
        .thenReturn(Uni.createFrom().failure(new IllegalArgumentException()));

    String patchDoc = "{\"status\":\"TESTING\"}";

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(patchDoc)
        .when()
        .patch(productId)
        .then()
        .statusCode(405)
        .contentType(ContentType.JSON)
        .body("title", equalTo("HTTP 405 Method Not Allowed"))
        .body("detail", equalTo("HTTP 405 Method Not Allowed"))
        .body("status", equalTo(405));

    // then
    verify(productService, times(0))
        .patchProductById(eq(productId), eq("createdBy"), any(ProductPatchRequest.class));
    verifyNoMoreInteractions(productService);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void patchProductTest_shouldReturn404_whenProductNotFound() {
    // given
    String productId = "prod-test";

    when(productService.patchProductById(
            eq(productId), eq("createdBy"), any(ProductPatchRequest.class)))
        .thenReturn(Uni.createFrom().failure(new NotFoundException()));

    String patchDoc = "{\"productId\":\"prod-test\"}";

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(patchDoc)
        .when()
        .patch(productId)
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("detail", equalTo("No product found with productId: " + productId))
        .body("status", equalTo(404))
        .body("instance", equalTo("/products/" + productId));

    // then
    verify(productService, times(1))
        .patchProductById(eq(productId), eq("createdBy"), any(ProductPatchRequest.class));
    verifyNoMoreInteractions(productService);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void patchProductTest_shouldReturn500_whenRuntimeError() {
    // given
    String productId = "prod-test";

    when(productService.patchProductById(
            eq(productId), eq("createdBy"), any(ProductPatchRequest.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException()));

    String patchDoc = "{}";

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(patchDoc)
        .when()
        .patch(productId)
        .then()
        .statusCode(500)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Internal Server Error"))
        .body("status", equalTo(500))
        .body("instance", equalTo("/products/" + productId));

    // then
    verify(productService, times(1))
        .patchProductById(eq(productId), eq("createdBy"), any(ProductPatchRequest.class));
    verifyNoMoreInteractions(productService);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void patchProductTest_shouldReturn400_whenInvalidPatchDocument() {
    // given
    String productId = "prod-test";
    String invalidPayload = "{}";

    when(productService.patchProductById(
            eq(productId), eq("createdBy"), any(ProductPatchRequest.class)))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new BadRequestException(
                        "Invalid patch payload or field constraints violated")));

    // when
    given()
        .queryParam("createdBy", "createdBy")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(invalidPayload)
        .when()
        .patch(productId)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Bad Request"))
        .body("detail", equalTo("Invalid patch payload or field constraints violated"))
        .body("status", equalTo(400))
        .body("instance", equalTo("/products/" + productId));

    // then
    verify(productService, times(1))
        .patchProductById(eq(productId), eq("createdBy"), any(ProductPatchRequest.class));
    verifyNoMoreInteractions(productService);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProductOriginsById_shouldReturnOk() {
    // given
    String productId = "prod-test";
    ProductOriginResponse response =
        ProductOriginResponse.builder()
            .origins(
                List.of(
                    OriginEntry.builder()
                        .institutionType(InstitutionType.PA)
                        .labelKey("pa")
                        .origin(Origin.IPA)
                        .build()))
            .build();

    when(productService.getProductOriginsById(productId))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .get("/origins")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("origins[0].institutionType", equalTo(InstitutionType.PA.name()))
        .body("origins[0].labelKey", equalTo("pa"))
        .body("origins[0].origin", equalTo(Origin.IPA.name()));

    // then
    verify(productService, times(1)).getProductOriginsById(productId);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProductOriginsById_whenNotFound_shouldReturnKO() {
    // given
    String productId = "prod-test";

    when(productService.getProductOriginsById(productId))
        .thenReturn(Uni.createFrom().failure(new NotFoundException()));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("createdBy", "createdBy")
        .accept(ContentType.JSON)
        .when()
        .get("/origins")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", containsString(productId))
        .body("instance", equalTo("/products/" + productId + "/origins"));

    // then
    verify(productService, times(1)).getProductOriginsById(productId);
  }

  // -------------------------------------------------------------------------
  // GET /workflow-type
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void getWorkflowType_shouldReturn200_whenRuleFound() {
    // given
    String productId = "prod-test";
    WorkflowTypeResponse response =
        WorkflowTypeResponse.builder().workflowType(WorkflowType.CONTRACT_REGISTRATION).build();

    when(productService.getWorkflowType(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get("/workflow-type")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("workflowType", equalTo(WorkflowType.CONTRACT_REGISTRATION.name()));

    // then
    verify(productService, times(1)).getWorkflowType(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getWorkflowType_shouldReturn200() {
    // given
    String productId = "prod-test";
    WorkflowTypeResponse response =
        WorkflowTypeResponse.builder().workflowType(WorkflowType.FOR_APPROVE).build();

    when(productService.getWorkflowType(productId, InstitutionType.GSP, Origin.SELC))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("institutionType", "GSP")
        .queryParam("origin", "SELC")
        .accept(ContentType.JSON)
        .when()
        .get("/workflow-type")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("workflowType", equalTo(WorkflowType.FOR_APPROVE.name()));

    // then
    verify(productService, times(1)).getWorkflowType(productId, InstitutionType.GSP, Origin.SELC);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getWorkflowType_shouldReturn404_whenProductNotFound() {
    // given
    String productId = "prod-missing";

    when(productService.getWorkflowType(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(
            Uni.createFrom()
                .failure(new NotFoundException("Product prod-missing not found")));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get("/workflow-type")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Not Found"))
        .body("status", equalTo(404))
        .body("detail", containsString(productId));

    // then
    verify(productService, times(1)).getWorkflowType(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getWorkflowType_shouldReturn404_whenNoRuleMatches() {
    // given
    String productId = "prod-test";

    when(productService.getWorkflowType(productId, InstitutionType.GSP, Origin.SELC))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new NotFoundException(
                        "No workflowRule found for product prod-test, institutionType GSP, origin SELC")));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("institutionType", "GSP")
        .queryParam("origin", "SELC")
        .accept(ContentType.JSON)
        .when()
        .get("/workflow-type")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Not Found"))
        .body("status", equalTo(404))
        .body("detail", containsString("GSP"));

    // then
    verify(productService, times(1)).getWorkflowType(productId, InstitutionType.GSP, Origin.SELC);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getWorkflowType_shouldReturn400_whenServiceThrowsBadRequest() {
    // given
    String productId = "prod-test";

    when(productService.getWorkflowType(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(
            Uni.createFrom()
                .failure(new IllegalArgumentException("Missing origin")));

    // when
    given()
        .queryParam("productId", productId)
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get("/workflow-type")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400));

    // then
    verify(productService, times(1)).getWorkflowType(productId, InstitutionType.PA, Origin.IPA);
  }

  // -------------------------------------------------------------------------
  // HEAD /{productId}/required-documents/enabled
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void isRequiredDocumentsEnabled_shouldReturn200_withHeaderTrue() {
    // given
    String productId = "prod-test";

    when(productService.isRequiredDocumentsEnabled(productId, InstitutionType.GSP, Origin.SELC))
        .thenReturn(Uni.createFrom().item(true));

    // when
    given()
        .queryParam("institutionType", "GSP")
        .queryParam("origin", "SELC")
        .when()
        .head(productId + "/required-documents/enabled")
        .then()
        .statusCode(200)
        .header("X-Required-Documents-Enabled", "true");

    // then
    verify(productService, times(1))
        .isRequiredDocumentsEnabled(productId, InstitutionType.GSP, Origin.SELC);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void isRequiredDocumentsEnabled_shouldReturn200_withHeaderFalse() {
    // given
    String productId = "prod-test";

    when(productService.isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(Uni.createFrom().item(false));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .when()
        .head(productId + "/required-documents/enabled")
        .then()
        .statusCode(200)
        .header("X-Required-Documents-Enabled", "false");

    // then
    verify(productService, times(1))
        .isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void isRequiredDocumentsEnabled_shouldReturn404_whenProductNotFound() {
    // given
    String productId = "prod-missing";

    when(productService.isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(
            Uni.createFrom()
                .failure(new NotFoundException("Product prod-missing not found")));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .when()
        .head(productId + "/required-documents/enabled")
        .then()
        .statusCode(404);

    // then
    verify(productService, times(1))
        .isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void isRequiredDocumentsEnabled_shouldReturn400_whenServiceThrowsIllegalArgument() {
    // given
    String productId = "prod-test";

    when(productService.isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(
            Uni.createFrom()
                .failure(new IllegalArgumentException("Missing productId")));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .when()
        .head(productId + "/required-documents/enabled")
        .then()
        .statusCode(400);

    // then
    verify(productService, times(1))
        .isRequiredDocumentsEnabled(productId, InstitutionType.PA, Origin.IPA);
  }

  // -------------------------------------------------------------------------
  // GET /{productId}/required-documents
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void getRequiredDocuments_shouldReturn200_withList() {
    // given
    String productId = "prod-test";
    List<RequiredDocumentResponse> documents =
        List.of(
            RequiredDocumentResponse.builder()
                .id("statuto")
                .name("Statuto Ente")
                .labelKey("statuto")
                .required(true)
                .mimeType("application/pdf")
                .maxDocumentsRequired(1)
                .build(),
            RequiredDocumentResponse.builder()
                .id("attestazione-gsp")
                .name("Attestazione")
                .labelKey("attestazione-gsp")
                .required(true)
                .mimeType("application/pdf")
                .maxDocumentsRequired(3)
                .build());

    when(productService.getRequiredDocuments(productId, InstitutionType.GSP, Origin.SELC))
        .thenReturn(Uni.createFrom().item(documents));

    // when
    given()
        .queryParam("institutionType", "GSP")
        .queryParam("origin", "SELC")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/required-documents")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", equalTo(2))
        .body("[0].id", equalTo("statuto"))
        .body("[0].name", equalTo("Statuto Ente"))
        .body("[0].maxDocumentsRequired", equalTo(1))
        .body("[1].id", equalTo("attestazione-gsp"))
        .body("[1].maxDocumentsRequired", equalTo(3));

    // then
    verify(productService, times(1))
        .getRequiredDocuments(productId, InstitutionType.GSP, Origin.SELC);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getRequiredDocuments_shouldReturn200_withEmptyList() {
    // given
    String productId = "prod-test";

    when(productService.getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(Uni.createFrom().item(List.of()));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/required-documents")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", equalTo(0));

    // then
    verify(productService, times(1))
        .getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getRequiredDocuments_shouldReturn404_whenProductNotFound() {
    // given
    String productId = "prod-missing";

    when(productService.getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(
            Uni.createFrom().failure(new NotFoundException("Product prod-missing not found")));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/required-documents")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", containsString(productId))
        .body("instance", equalTo("/product/" + productId + "/required-documents"));

    // then
    verify(productService, times(1))
        .getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getRequiredDocuments_shouldReturn400_whenServiceThrowsIllegalArgument() {
    // given
    String productId = "prod-test";

    when(productService.getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA))
        .thenReturn(Uni.createFrom().failure(new IllegalArgumentException("Missing origin")));

    // when
    given()
        .queryParam("institutionType", "PA")
        .queryParam("origin", "IPA")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/required-documents")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", equalTo("Missing origin"));

    // then
    verify(productService, times(1))
        .getRequiredDocuments(productId, InstitutionType.PA, Origin.IPA);
  }

  // UTILS
  private ProductCreateRequest getProductCreateRequest() {
    ProductCreateRequest productCreateRequest = null;
    try {
      productCreateRequest =
          objectMapper.readValue(
              getClass().getResource("/request/createRequest.json"), ProductCreateRequest.class);
    } catch (IOException e) {
      log.error("Error", e);
    }
    return productCreateRequest;
  }

  private ProductResponse getProductResponse() {
    ProductResponse productResponse = null;
    try {
      productResponse =
          objectMapper.readValue(
              getClass().getResource("/request/createRequest.json"), ProductResponse.class);
    } catch (IOException e) {
      log.error("Error", e);
    }
    return productResponse;
  }

  // -------------------------------------------------------------------------
  // GET /{productId}/valid
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void getValidProductById_shouldReturn200() {
    // given
    String productId = "prod-test";
    ProductResponse response = new ProductResponse();
    response.setProductId(productId);
    response.setStatus(ProductStatus.ACTIVE);

    when(productService.getValidProductById(productId))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/valid")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("productId", equalTo(productId))
        .body("status", equalTo(ProductStatus.ACTIVE.name()));

    // then
    verify(productService, times(1)).getValidProductById(productId);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getValidProductById_shouldReturn404_whenNotValid() {
    // given
    String productId = "prod-ced";

    when(productService.getValidProductById(productId))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new NotFoundException("Product with id prod-ced has status INACTIVE")));

    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/valid")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", equalTo("Product with id prod-ced has status INACTIVE"))
        .body("instance", equalTo("/product/" + productId + "/valid"));

    // then
    verify(productService, times(1)).getValidProductById(productId);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getValidProductById_shouldReturn404_whenNotFound() {
    // given
    String productId = "prod-missing";

    when(productService.getValidProductById(productId))
        .thenReturn(
            Uni.createFrom().failure(new NotFoundException("Product prod-missing not found")));

    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/valid")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", containsString(productId))
        .body("instance", equalTo("/product/" + productId + "/valid"));

    // then
    verify(productService, times(1)).getValidProductById(productId);
  }

  // -------------------------------------------------------------------------
  // GET /{productId}/expiration-days
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void getProductExpirationDays_shouldReturn200() {
    // given
    String productId = "prod-test";
    ProductExpirationResponse response =
        ProductExpirationResponse.builder().expirationDays(60).build();

    when(productService.getProductExpirationDays(productId))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/expiration-days")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("expirationDays", equalTo(60));

    // then
    verify(productService, times(1)).getProductExpirationDays(productId);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProductExpirationDays_shouldReturn404_whenNotValid() {
    // given
    String productId = "prod-ced";

    when(productService.getProductExpirationDays(productId))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new NotFoundException("Product with id prod-ced has status INACTIVE")));

    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/expiration-days")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Product not found"))
        .body("status", equalTo(404))
        .body("detail", equalTo("Product with id prod-ced has status INACTIVE"))
        .body("instance", equalTo("/product/" + productId + "/expiration-days"));

    // then
    verify(productService, times(1)).getProductExpirationDays(productId);
  }

  // -------------------------------------------------------------------------
  // GET /product (list, latest version per productId)
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void getProducts_shouldReturn200_withList() {
    // given
    ProductResponse root = new ProductResponse();
    root.setProductId("prod-a");
    root.setStatus(ProductStatus.ACTIVE);
    ProductResponse other = new ProductResponse();
    other.setProductId("prod-b");
    other.setStatus(ProductStatus.ACTIVE);

    when(productService.getProducts(true, true))
        .thenReturn(Uni.createFrom().item(List.of(root, other)));

    // when
    given()
        .queryParam("rootOnly", "true")
        .queryParam("valid", "true")
        .accept(ContentType.JSON)
        .when()
        .get()
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", equalTo(2))
        .body("[0].productId", equalTo("prod-a"))
        .body("[1].productId", equalTo("prod-b"));

    // then
    verify(productService, times(1)).getProducts(true, true);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getProducts_shouldReturn400_whenQueryParamsMissing() {
    // when
    given()
        .accept(ContentType.JSON)
        .when()
        .get()
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", equalTo("Query params 'rootOnly' and 'valid' are required"))
        .body("instance", equalTo("/product"));

    // then
    verifyNoInteractions(productService);
  }

  // -------------------------------------------------------------------------
  // GET /{productId}/role-mappings/validate
  // -------------------------------------------------------------------------

  @Test
  @TestSecurity(user = "userJwt")
  void validateProductRole_shouldReturn200() {
    // given
    String productId = "prod-test";
    ProductRoleResponse response =
        ProductRoleResponse.builder().code("admin").label("Admin").description("desc").build();

    when(productService.validateProductRole(productId, UserRole.MANAGER, "admin"))
        .thenReturn(Uni.createFrom().item(response));

    // when
    given()
        .queryParam("role", "MANAGER")
        .queryParam("productRole", "admin")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/role-mappings/validate")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("code", equalTo("admin"))
        .body("label", equalTo("Admin"));

    // then
    verify(productService, times(1)).validateProductRole(productId, UserRole.MANAGER, "admin");
  }

  @Test
  @TestSecurity(user = "userJwt")
  void validateProductRole_shouldReturn404_whenNotFound() {
    // given
    String productId = "prod-test";

    when(productService.validateProductRole(productId, UserRole.MANAGER, "missing"))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new NotFoundException(
                        "ProductRole missing not found for role MANAGER in product prod-test")));

    // when
    given()
        .queryParam("role", "MANAGER")
        .queryParam("productRole", "missing")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/role-mappings/validate")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Not Found"))
        .body("status", equalTo(404))
        .body("detail", containsString("missing"))
        .body("instance", equalTo("/product/" + productId + "/role-mappings/validate"));

    // then
    verify(productService, times(1)).validateProductRole(productId, UserRole.MANAGER, "missing");
  }

  @Test
  @TestSecurity(user = "userJwt")
  void validateProductRole_shouldReturn400_whenProductRoleMissing() {
    // given
    String productId = "prod-test";

    when(productService.validateProductRole(productId, UserRole.MANAGER, null))
        .thenReturn(
            Uni.createFrom().failure(new BadRequestException("Missing productRole")));

    // when
    given()
        .queryParam("role", "MANAGER")
        .accept(ContentType.JSON)
        .when()
        .get(productId + "/role-mappings/validate")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", equalTo("Missing productRole"))
        .body("instance", equalTo("/product/" + productId + "/role-mappings/validate"));

    // then
    verify(productService, times(1)).validateProductRole(productId, UserRole.MANAGER, null);
  }

}
