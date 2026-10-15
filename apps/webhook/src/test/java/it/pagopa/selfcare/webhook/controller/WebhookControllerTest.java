package it.pagopa.selfcare.webhook.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.exception.WebhookAlreadyExistsException;
import it.pagopa.selfcare.webhook.service.WebhookService;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class WebhookControllerTest {

  public static final String PROD_TEST = "prod-test";
  public static final String TENANT_ID = "SELC";
  public static final String URL = "http://example.com";
  @InjectMock WebhookService webhookService;

  @Test
  void createWebhook_shouldReturnCreated() {
    String url = URL;
    ;

    WebhookRequest request = new WebhookRequest();
    request.setUrl(url);
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);

    WebhookResponse response = new WebhookResponse();
    response.setTenantId(TENANT_ID);
    response.setUrl(url);

    Mockito.when(webhookService.createWebhook(any(WebhookRequest.class)))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("requesterProductId", PROD_TEST)
        .body(request)
        .when()
        .post("/webhooks")
        .then()
        .statusCode(201)
        .body("url", equalTo(url));
  }

  @Test
  void createWebhook_shouldReturnConflictWhenWebhookWithSameProductAndTenantAlreadyExists() {
    // given
    WebhookRequest request = new WebhookRequest();
    request.setUrl(URL);
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);
    Mockito.when(webhookService.createWebhook(any(WebhookRequest.class)))
        .thenReturn(
            Uni.createFrom()
                .failure(new WebhookAlreadyExistsException("Webhook already exists")));

    // when
    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/webhooks")
        .then()
        .statusCode(409);
  }

  @Test
  void listWebhooks_shouldReturnList() {
    WebhookResponse response = new WebhookResponse();
    response.setTenantId(TENANT_ID);
    response.setProductId(PROD_TEST);

    Mockito.when(webhookService.listWebhooks(TENANT_ID, 0, 20))
        .thenReturn(Uni.createFrom().item(List.of(response)));

    given()
        .queryParam("tenantId", TENANT_ID)
        .when()
        .get("/webhooks")
        .then()
        .statusCode(200)
        .body("$.size()", is(1))
        .body("[0].tenantId", equalTo(TENANT_ID))
        .body("[0].productId", equalTo(PROD_TEST));
  }

  @Test
  void listWebhooks_shouldApplyPaginationParameters() {
    // given
    Mockito.when(webhookService.listWebhooks(TENANT_ID, 2, 5))
        .thenReturn(Uni.createFrom().item(List.of()));

    // when
    given()
        .queryParam("tenantId", TENANT_ID)
        .queryParam("page", 2)
        .queryParam("size", 5)
        .when()
        .get("/webhooks")
        .then()
        .statusCode(200);

    // then
    Mockito.verify(webhookService).listWebhooks(TENANT_ID, 2, 5);
  }

  @Test
  void listWebhooks_shouldReturnAllTenants_whenTenantIdIsNotProvided() {
    // given
    Mockito.when(webhookService.listWebhooks(null, 0, 20))
        .thenReturn(Uni.createFrom().item(List.of()));

    // when
    given().when().get("/webhooks").then().statusCode(200);

    // then
    Mockito.verify(webhookService).listWebhooks(null, 0, 20);
  }

  @Test
  void getWebhookByProductId_shouldReturnWebhook_whenFound() {
    WebhookResponse response = new WebhookResponse();
    response.setTenantId(TENANT_ID);
    response.setProductId(PROD_TEST);

    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST, TENANT_ID))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .when()
        .queryParam("tenantId", TENANT_ID)
        .queryParam("requesterProductId", PROD_TEST)
        .get("/webhooks/" + PROD_TEST)
        .then()
        .statusCode(200)
        .body("tenantId", equalTo(TENANT_ID))
        .body("productId", equalTo(PROD_TEST));
  }

  @Test
  void getWebhookByProductId_shouldReturnNotFound_whenNotFound() {
    Mockito.when(webhookService.getWebhookByProductId("999", TENANT_ID))
        .thenReturn(Uni.createFrom().nullItem());

    given().queryParam("tenantId", TENANT_ID).when().get("/webhooks/999").then().statusCode(404);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "tenant_id", value = "AR")})
  void getWebhookByProductId_shouldUseJwtTenant_whenTenantIdIsMissing() {
    WebhookResponse response = new WebhookResponse();
    response.setTenantId("AR");
    response.setProductId(PROD_TEST);

    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST, "AR"))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .when()
        .get("/webhooks/" + PROD_TEST)
        .then()
        .statusCode(200)
        .body("tenantId", equalTo("AR"));
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "tenant_id", value = "AR")})
  void getWebhookByProductId_shouldRejectQueryTenant_conflictingWithJwtTenant() {
    given().queryParam("tenantId", "PNPG").when().get("/webhooks/" + PROD_TEST).then().statusCode(400);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  void getWebhookByProductId_shouldUseHeaderTenant_whenQueryAndJwtAreMissing() {
    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST, "AR"))
        .thenReturn(Uni.createFrom().nullItem());

    given().header("X-Tenant-Id", "AR").when().get("/webhooks/" + PROD_TEST).then().statusCode(404);

    Mockito.verify(webhookService).getWebhookByProductId(PROD_TEST, "AR");
  }

  @Test
  void getWebhookByProductId_shouldRejectQueryTenant_conflictingWithHeaderTenant() {
    given()
        .header("X-Tenant-Id", "AR")
        .queryParam("tenantId", "PNPG")
        .when()
        .get("/webhooks/" + PROD_TEST)
        .then()
        .statusCode(400);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "tenant_id", value = "AR")})
  void getWebhookByProductId_shouldRejectHeaderTenant_conflictingWithJwtTenant() {
    given().header("X-Tenant-Id", "PNPG").when().get("/webhooks/" + PROD_TEST).then().statusCode(400);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "tenant_id", value = "AR")})
  void getWebhookByProductId_shouldAcceptMatchingQueryHeaderAndJwt() {
    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST, "AR"))
        .thenReturn(Uni.createFrom().nullItem());

    given()
        .header("X-Tenant-Id", "AR")
        .queryParam("tenantId", "AR")
        .when()
        .get("/webhooks/" + PROD_TEST)
        .then()
        .statusCode(404);

    Mockito.verify(webhookService).getWebhookByProductId(PROD_TEST, "AR");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {
      @Claim(key = "iss", value = "SPID"),
      @Claim(key = "tenant_id", value = "AR")
  })
  void getWebhookByProductId_shouldAcceptSpidToken_withMatchingHeader() {
    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST, "AR"))
        .thenReturn(Uni.createFrom().nullItem());

    given().header("X-Tenant-Id", "AR").when().get("/webhooks/" + PROD_TEST).then().statusCode(404);

    Mockito.verify(webhookService).getWebhookByProductId(PROD_TEST, "AR");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {
      @Claim(key = "iss", value = "SPID"),
      @Claim(key = "tenant_id", value = "AR")
  })
  void getWebhookByProductId_shouldRejectSpidToken_withoutHeader() {
    given().when().get("/webhooks/" + PROD_TEST).then().statusCode(401);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {
      @Claim(key = "iss", value = "SPID"),
      @Claim(key = "tenant_id", value = "AR")
  })
  void getWebhookByProductId_shouldRejectSpidToken_withConflictingHeader() {
    given().header("X-Tenant-Id", "PNPG").when().get("/webhooks/" + PROD_TEST).then().statusCode(401);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  void getWebhookByProductId_shouldReturnBadRequest_whenTenantIsMissingEverywhere() {
    given().when().get("/webhooks/" + PROD_TEST).then().statusCode(400);

    Mockito.verify(webhookService, Mockito.never()).getWebhookByProductId(any(), any());
  }

  @Test
  void updateWebhook_shouldReturnOk_whenUpdated() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl(URL);
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);

    WebhookResponse response = new WebhookResponse();
    response.setTenantId(TENANT_ID);
    response.setProductId(PROD_TEST);
    response.setUrl(URL);

    Mockito.when(webhookService.updateWebhook(any(WebhookRequest.class), eq(PROD_TEST)))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .put("/webhooks/" + PROD_TEST)
        .then()
        .statusCode(200)
        .body("url", equalTo(URL));
  }

  @Test
  void updateWebhook_shouldReturnNotFound_whenIdDoesNotExist() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl(URL);
    request.setHttpMethod("POST");
    request.setTenantId(TENANT_ID);
    request.setProductId(PROD_TEST);

    Mockito.when(webhookService.updateWebhook(any(WebhookRequest.class), eq("999")))
        .thenReturn(Uni.createFrom().failure(new IllegalArgumentException("Webhook not found")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .put("/webhooks/999")
        .then()
        .statusCode(404);
  }

  @Test
  void sendNotification_shouldReturnAccepted() {
    NotificationRequest request = new NotificationRequest();
    request.setProductId(PROD_TEST);
    request.setTenantId(TENANT_ID);
    request.setPayload("{\"event\":\"test\"}");
    request.setTopic("SC-Contracts");

    Mockito.when(webhookService.sendNotification(any(NotificationRequest.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/webhooks/notify")
        .then()
        .statusCode(202);
  }
}
