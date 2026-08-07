package it.pagopa.selfcare.webhook.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.service.WebhookService;
import it.pagopa.selfcare.webhook.util.Sanitizer;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/webhooks/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Webhook Notification", description = "API for resending webhook notifications")
public class WebhookNotificationController {

  @Inject WebhookService webhookService;

  @POST
  @Path("/{notificationId}/resend")
  @Operation(
      summary = "Resend a notification by ID",
      description = "Reset a single notification to PENDING and re-publish it for delivery",
      operationId = "resendNotificationById")
  @Tag(name = "internal-v1")
  public Uni<Response> resendById(@PathParam("notificationId") String notificationId) {
    return webhookService
        .resendNotificationById(Sanitizer.sanitizeString(notificationId))
        .map(response -> Response.ok(response).build())
        .onFailure(IllegalArgumentException.class)
        .recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  @Path("/resend")
  @Operation(
      summary = "Resend notifications by status",
      description =
          "Reset every notification matching the given status (optionally restricted to a"
              + " single webhook) to PENDING and re-publish them for delivery",
      operationId = "resendNotificationsByStatus")
  @Tag(name = "internal-v1")
  public Uni<Response> resendByStatus(
      @DefaultValue("FAILED") @QueryParam("status") WebhookNotification.NotificationStatus status,
      @QueryParam("webhookId") String webhookId) {
    return webhookService
        .resendNotificationsByStatus(status, Sanitizer.sanitizeString(webhookId))
        .map(response -> Response.ok(response).build())
        .onFailure(IllegalArgumentException.class)
        .recoverWithItem(Response.status(Response.Status.BAD_REQUEST).build());
  }

  @POST
  @Path("/resend/date-range")
  @Operation(
      summary = "Resend notifications created within a date range",
      description =
          "Reset every notification created between the given ISO-8601 date-times (e.g."
              + " 2024-01-01T00:00:00) to PENDING and re-publish them for delivery",
      operationId = "resendNotificationsByDateRange")
  @Tag(name = "internal-v1")
  public Uni<Response> resendByDateRange(
      @NotBlank @QueryParam("from") String from, @NotBlank @QueryParam("to") String to) {
    LocalDateTime fromDate;
    LocalDateTime toDate;
    try {
      fromDate = LocalDateTime.parse(from);
      toDate = LocalDateTime.parse(to);
    } catch (DateTimeParseException e) {
      return Uni.createFrom()
          .item(
              Response.status(Response.Status.BAD_REQUEST)
                  .entity("Invalid date format, expected ISO-8601 (e.g. 2024-01-01T00:00:00)")
                  .build());
    }
    return webhookService
        .resendNotificationsByDateRange(fromDate, toDate)
        .map(response -> Response.ok(response).build());
  }
}
