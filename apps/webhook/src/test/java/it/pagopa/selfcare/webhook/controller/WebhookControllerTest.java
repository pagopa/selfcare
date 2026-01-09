package it.pagopa.selfcare.webhook.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.service.WebhookService;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class WebhookControllerTest {

  public static final String PROD_TEST = "prod-test";
  public static final String URL = "http://example.com";
  @InjectMock
  WebhookService webhookService;

  @Test
  void createWebhook_shouldReturnCreated() {
    String url = URL;;

    WebhookRequest request = new WebhookRequest();
    request.setUrl(url);
    request.setHttpMethod("POST");
    request.setProductId(PROD_TEST);

    WebhookResponse response = new WebhookResponse();
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
  void listWebhooks_shouldReturnList() {
    WebhookResponse response = new WebhookResponse();
    response.setProductId(PROD_TEST);

    Mockito.when(webhookService.listWebhooks())
      .thenReturn(Uni.createFrom().item(List.of(response)));

    given()
      .when()
      .get("/webhooks")
      .then()
      .statusCode(200)
      .body("$.size()", is(1))
      .body("[0].productId", equalTo(PROD_TEST));
  }

  @Test
  void getWebhookByProductId_shouldReturnWebhook_whenFound() {
    WebhookResponse response = new WebhookResponse();
    response.setProductId(PROD_TEST);

    Mockito.when(webhookService.getWebhookByProductId(PROD_TEST))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .when()
      .queryParam("requesterProductId", PROD_TEST)
      .get("/webhooks/"+PROD_TEST)
      .then()
      .statusCode(200)
      .body("productId", equalTo(PROD_TEST));
  }

  @Test
  void getWebhookByProductId_shouldReturnNotFound_whenNotFound() {
    Mockito.when(webhookService.getWebhookByProductId("999"))
      .thenReturn(Uni.createFrom().nullItem());

    given()
      .when()
      .get("/webhooks/999")
      .then()
      .statusCode(404);
  }

  @Test
  void updateWebhook_shouldReturnOk_whenUpdated() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl(URL);
    request.setHttpMethod("POST");
    request.setProductId(PROD_TEST);

    WebhookResponse response = new WebhookResponse();
    response.setProductId(PROD_TEST);
    response.setUrl(URL);

    Mockito.when(webhookService.updateWebhook(any(WebhookRequest.class), eq(PROD_TEST)))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .put("/webhooks/"+PROD_TEST)
      .then()
      .statusCode(200)
      .body("url", equalTo(URL));
  }

  @Test
  void updateWebhook_shouldReturnNotFound_whenIdDoesNotExist() {
    WebhookRequest request = new WebhookRequest();
    request.setUrl(URL);
    request.setHttpMethod("POST");
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
    request.setPayload("{\"event\":\"test\"}");

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