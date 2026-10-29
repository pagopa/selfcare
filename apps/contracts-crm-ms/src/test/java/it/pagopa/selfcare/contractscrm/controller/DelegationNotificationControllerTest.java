package it.pagopa.selfcare.contractscrm.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.exception.ResourceNotFoundException;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationNotification;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationWebhookRequest;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import it.pagopa.selfcare.contractscrm.service.ContractDelegationService;
import it.pagopa.selfcare.contractscrm.testsupport.JwtTestResource;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(DelegationNotificationController.class)
@QuarkusTestResource(JwtTestResource.class)
class DelegationNotificationControllerTest {

  @InjectMock ContractDelegationService contractDelegationService;

  @Test
  void notifyDelegation_withoutToken_returns401() {
    given()
        .contentType(ContentType.JSON)
        .body(request())
        .when()
        .post("/notify")
        .then()
        .statusCode(401);
    verify(contractDelegationService, never()).processDelegation(any());
  }

  @Test
  void notifyDelegation_withWrongAudience_returns401() {
    given()
        .auth()
        .oauth2(JwtTestResource.token("another-audience"))
        .contentType(ContentType.JSON)
        .body(request())
        .when()
        .post("/notify")
        .then()
        .statusCode(401);
  }

  @Test
  void notifyDelegation_pspDelegation_returns200() {
    ContractDelegationResponse response = new ContractDelegationResponse();
    response.setId("contract-delegation-id");
    when(contractDelegationService.processDelegation(any()))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .auth()
        .oauth2(JwtTestResource.validToken())
        .contentType(ContentType.JSON)
        .body(request())
        .when()
        .post("/notify")
        .then()
        .statusCode(200)
        .body("id", equalTo("contract-delegation-id"));
  }

  @Test
  void notifyDelegation_notHandledDelegation_returns204() {
    when(contractDelegationService.processDelegation(any())).thenReturn(Uni.createFrom().nullItem());

    given()
        .auth()
        .oauth2(JwtTestResource.validToken())
        .contentType(ContentType.JSON)
        .body(request())
        .when()
        .post("/notify")
        .then()
        .statusCode(204);
  }

  @Test
  void notifyDelegation_withoutPayload_returns400() {
    given()
        .auth()
        .oauth2(JwtTestResource.validToken())
        .contentType(ContentType.JSON)
        .body("{\"topic\":\"SC-Delegations\"}")
        .when()
        .post("/notify")
        .then()
        .statusCode(400);
    verify(contractDelegationService, never()).processDelegation(any());
  }

  @Test
  void notifyDelegation_contractNotFound_returns404Problem() {
    when(contractDelegationService.processDelegation(any()))
        .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("not found")));

    given()
        .auth()
        .oauth2(JwtTestResource.validToken())
        .contentType(ContentType.JSON)
        .body(request())
        .when()
        .post("/notify")
        .then()
        .statusCode(404)
        .contentType("application/problem+json")
        .body("detail", equalTo("not found"));
  }

  private static DelegationWebhookRequest request() {
    return DelegationWebhookRequest.builder()
        .tenantId("AR")
        .productId("prod-pagopa")
        .topic("SC-Delegations")
        .payload(
            DelegationNotification.builder()
                .delegationId("delegation-id")
                .from("from-institution")
                .to("to-institution")
                .productId("prod-pagopa")
                .type(DelegationNotification.TYPE_PSP_MEMBERSHIP)
                .status("ACTIVE")
                .build())
        .build();
  }
}
