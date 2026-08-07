package it.pagopa.selfcare.webhook.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationResendResponse;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.service.WebhookService;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class WebhookNotificationControllerTest {

  public static final String NOTIFICATION_ID = "6a743ae3f3b725087146d223";
  public static final String WEBHOOK_ID = "6a743ae3f3b725087146d226";

  @InjectMock WebhookService webhookService;

  @Test
  void resendById_shouldReturnOk_whenFound() {
    NotificationResendResponse response = new NotificationResendResponse();
    response.setResentCount(1);
    response.setNotificationIds(List.of(NOTIFICATION_ID));

    Mockito.when(webhookService.resendNotificationById(NOTIFICATION_ID))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/webhooks/notifications/" + NOTIFICATION_ID + "/resend")
        .then()
        .statusCode(200)
        .body("resentCount", is(1))
        .body("notificationIds[0]", equalTo(NOTIFICATION_ID));
  }

  @Test
  void resendById_shouldReturnNotFound_whenNotificationDoesNotExist() {
    Mockito.when(webhookService.resendNotificationById(NOTIFICATION_ID))
        .thenReturn(
            Uni.createFrom()
                .failure(
                    new IllegalArgumentException("Notification not found: " + NOTIFICATION_ID)));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/webhooks/notifications/" + NOTIFICATION_ID + "/resend")
        .then()
        .statusCode(404);
  }

  @Test
  void resendByStatus_shouldDefaultToFailedStatus() {
    NotificationResendResponse response = new NotificationResendResponse();
    response.setResentCount(2);
    response.setNotificationIds(List.of("id1", "id2"));

    Mockito.when(
            webhookService.resendNotificationsByStatus(
                eq(WebhookNotification.NotificationStatus.FAILED), isNull()))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/webhooks/notifications/resend")
        .then()
        .statusCode(200)
        .body("resentCount", is(2));

    Mockito.verify(webhookService)
        .resendNotificationsByStatus(WebhookNotification.NotificationStatus.FAILED, null);
  }

  @Test
  void resendByStatus_shouldApplyStatusAndWebhookIdQueryParams() {
    NotificationResendResponse response = new NotificationResendResponse();
    response.setResentCount(1);
    response.setNotificationIds(List.of(NOTIFICATION_ID));

    Mockito.when(
            webhookService.resendNotificationsByStatus(
                WebhookNotification.NotificationStatus.RETRY, WEBHOOK_ID))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("status", "RETRY")
        .queryParam("webhookId", WEBHOOK_ID)
        .when()
        .post("/webhooks/notifications/resend")
        .then()
        .statusCode(200)
        .body("resentCount", is(1));
  }

  @Test
  void resendByStatus_shouldReturnBadRequest_whenWebhookIdIsInvalid() {
    Mockito.when(webhookService.resendNotificationsByStatus(any(), any()))
        .thenReturn(
            Uni.createFrom()
                .failure(new IllegalArgumentException("Invalid webhook ID: not-an-id")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("webhookId", "not-an-id")
        .when()
        .post("/webhooks/notifications/resend")
        .then()
        .statusCode(400);
  }

  @Test
  void resendByDateRange_shouldReturnOk_whenDatesAreValid() {
    NotificationResendResponse response = new NotificationResendResponse();
    response.setResentCount(3);
    response.setNotificationIds(List.of("id1", "id2", "id3"));

    Mockito.when(
            webhookService.resendNotificationsByDateRange(
                LocalDateTime.parse("2024-01-01T00:00:00"),
                LocalDateTime.parse("2024-01-31T23:59:59")))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("from", "2024-01-01T00:00:00")
        .queryParam("to", "2024-01-31T23:59:59")
        .when()
        .post("/webhooks/notifications/resend/date-range")
        .then()
        .statusCode(200)
        .body("resentCount", is(3));
  }

  @Test
  void resendByDateRange_shouldReturnBadRequest_whenDateFormatIsInvalid() {
    given()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("from", "not-a-date")
        .queryParam("to", "2024-01-31T23:59:59")
        .when()
        .post("/webhooks/notifications/resend/date-range")
        .then()
        .statusCode(400);

    Mockito.verifyNoInteractions(webhookService);
  }
}
