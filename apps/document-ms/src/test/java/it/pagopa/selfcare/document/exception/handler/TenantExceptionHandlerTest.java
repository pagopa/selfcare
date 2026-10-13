package it.pagopa.selfcare.document.exception.handler;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.response.Problem;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class TenantExceptionHandlerTest {

  private final ExceptionHandler handler = new ExceptionHandler();

  @InjectMock DocumentService documentService;

  @Test
  void unknownTenantShouldMapToBadRequestWithoutTenantDetails() {
    Response response = handler.toResponse(new UnknownTenantException("SECRET-TENANT"));

    assertEquals(400, response.getStatus());
    assertEquals(ExceptionHandler.PROBLEM_JSON, response.getMediaType().toString());
    Problem problem = (Problem) response.getEntity();
    assertEquals(ExceptionHandler.INVALID_TENANT_CONTEXT, problem.getDetail());
    assertEquals(ExceptionHandler.INVALID_TENANT_CONTEXT, problem.getTitle());
    assertEquals(400, problem.getStatus());
  }

  @Test
  void unresolvedTenantShouldMapToUnauthorized() {
    Response response = handler.toResponse(new UnresolvedTenantException());

    assertEquals(401, response.getStatus());
    Problem problem = (Problem) response.getEntity();
    assertEquals(ExceptionHandler.INVALID_TENANT_CONTEXT, problem.getDetail());
    assertEquals(401, problem.getStatus());
  }

  @Test
  void unknownTenantFromServiceShouldReturnProblem400() {
    Mockito.when(documentService.getDocumentById(anyString()))
        .thenReturn(Uni.createFrom().failure(new UnknownTenantException("SECRET-TENANT")));

    given()
        .when().get("/v1/documents/doc-1")
        .then()
        .statusCode(400)
        .body("title", equalTo(ExceptionHandler.INVALID_TENANT_CONTEXT))
        .body(not(containsString("SECRET-TENANT")));
  }

  @Test
  void unresolvedTenantFromServiceShouldReturnProblem401() {
    Mockito.when(documentService.getDocumentById(anyString()))
        .thenReturn(Uni.createFrom().failure(new UnresolvedTenantException()));

    given()
        .when().get("/v1/documents/doc-1")
        .then()
        .statusCode(401)
        .body("title", equalTo(ExceptionHandler.INVALID_TENANT_CONTEXT));
  }
}
