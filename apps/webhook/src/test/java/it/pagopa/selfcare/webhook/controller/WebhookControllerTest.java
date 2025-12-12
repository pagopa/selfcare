package it.pagopa.selfcare.webhook.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
class WebhookControllerTest {

  @InjectMock
  WebhookService webhookService;

  @Test
  void createWebhook_shouldReturnCreated() {
    WebhookRequest request = new WebhookRequest();
    request.setName("Test Webhook");
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");

    WebhookResponse response = new WebhookResponse();
    response.setId("123");
    response.setName("Test Webhook");

    Mockito.when(webhookService.createWebhook(any(WebhookRequest.class)))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post("/webhooks")
      .then()
      .statusCode(201)
      .body("id", equalTo("123"))
      .body("name", equalTo("Test Webhook"));
  }

  @Test
  void listWebhooks_shouldReturnList() {
    WebhookResponse response = new WebhookResponse();
    response.setId("123");
    response.setName("Test Webhook");

    Mockito.when(webhookService.listWebhooks())
      .thenReturn(Uni.createFrom().item(List.of(response)));

    given()
      .when()
      .get("/webhooks")
      .then()
      .statusCode(200)
      .body("$.size()", is(1))
      .body("[0].id", equalTo("123"));
  }

  @Test
  void getWebhook_shouldReturnWebhook_whenFound() {
    WebhookResponse response = new WebhookResponse();
    response.setId("123");
    response.setName("Test Webhook");

    Mockito.when(webhookService.getWebhook("123"))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .when()
      .get("/webhooks/123")
      .then()
      .statusCode(200)
      .body("id", equalTo("123"));
  }

  @Test
  void getWebhook_shouldReturnNotFound_whenNotFound() {
    Mockito.when(webhookService.getWebhook("999"))
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
    request.setName("Updated Webhook");
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");

    WebhookResponse response = new WebhookResponse();
    response.setId("123");
    response.setName("Updated Webhook");

    Mockito.when(webhookService.updateWebhook(eq("123"), any(WebhookRequest.class)))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .put("/webhooks/123")
      .then()
      .statusCode(200)
      .body("name", equalTo("Updated Webhook"));
  }

  @Test
  void updateWebhook_shouldReturnNotFound_whenIdDoesNotExist() {
    WebhookRequest request = new WebhookRequest();
    request.setName("Updated Webhook");
    request.setUrl("http://example.com");
    request.setHttpMethod("POST");

    Mockito.when(webhookService.updateWebhook(eq("999"), any(WebhookRequest.class)))
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
  void deleteWebhook_shouldReturnNoContent_whenDeleted() {
    Mockito.when(webhookService.deleteWebhook("123"))
      .thenReturn(Uni.createFrom().item(true));

    given()
      .when()
      .delete("/webhooks/123")
      .then()
      .statusCode(204);
  }

  @Test
  void deleteWebhook_shouldReturnNotFound_whenIdDoesNotExist() {
    Mockito.when(webhookService.deleteWebhook("999"))
      .thenReturn(Uni.createFrom().failure(new IllegalArgumentException("Webhook not found")));

    given()
      .when()
      .delete("/webhooks/999")
      .then()
      .statusCode(404);
  }

  @Test
  void sendNotification_shouldReturnAccepted() {
    NotificationRequest request = new NotificationRequest();
    request.setProductId("prod-123");
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