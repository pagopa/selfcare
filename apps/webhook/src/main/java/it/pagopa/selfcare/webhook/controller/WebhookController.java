package it.pagopa.selfcare.webhook.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.service.WebhookService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Webhook", description = "API for webhook configuration management")
public class WebhookController {

  @Inject
  WebhookService webhookService;

  @POST
  @Operation(
      summary = "Create a new webhook",
      description = "Create a new webhook configuration",
      operationId = "createWebhook"
  )
  @Tag(name = "Webhook")
  @Tag(name = "external-v2")
  public Uni<Response> createWebhook(@Valid WebhookRequest request) {
    return webhookService.createWebhook(request)
        .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
  }

  @GET
  @Operation(
      summary = "List all webhooks",
      description = "Retrieve all webhook configurations",
      operationId = "listWebhooks"
  )
  @Tag(name = "Webhook")
  @Tag(name = "internal-v1")
  public Uni<List<WebhookResponse>> listWebhooks() {
    return webhookService.listWebhooks();
  }

  @GET
  @Path("/{productId}")
  @Operation(
      summary = "Get webhook by ID",
      description = "Retrieve a specific webhook configuration",
      operationId = "getWebhookbyProductId"
  )
  @Tag(name = "Webhook")
  @Tag(name = "external-v2")
  public Uni<Response> getWebhook(@PathParam("productId") String productId) {
    return webhookService.getWebhookByProductId(productId)
        .map(response -> response != null
            ? Response.ok(response).build()
            : Response.status(Response.Status.NOT_FOUND).build());
  }

  @PUT
  @Path("/{productId}")
  @Operation(
      summary = "Update webhook",
      description = "Update an existing webhook configuration",
      operationId = "updateWebhookbyProductId"
  )
  @Tag(name = "Webhook")
  @Tag(name = "external-v2")
  public Uni<Response> updateWebhook(@Valid WebhookRequest request,
      @PathParam("productId") String productId) {
    return webhookService.updateWebhook(request, productId)
        .map(response -> Response.ok(response).build())
        .onFailure(IllegalArgumentException.class)
        .recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
  }

  @DELETE
  @Path("/{productId}")
  @Operation(
      summary = "Delete webhook",
      description = "Delete a webhook configuration",
      operationId = "deleteWebhookbyProductId"
  )
  public Uni<Response> deleteWebhook(@PathParam("productId") String productId) {
    return Uni.createFrom().item(Response.status(Response.Status.NOT_IMPLEMENTED).build());
//    return webhookService.deleteWebhookByProductId(productId)
//      .map(deleted -> Response.noContent().build())
//      .onFailure(IllegalArgumentException.class)
//      .recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
  }

  @POST
  @Path("/notify")
  @Tag(name = "Webhook")
  @Tag(name = "internal-v1")
  @Operation(
      summary = "Send notification",
      description = "Create and send a webhook notification",
      operationId = "sendNotification"
  )
  public Uni<Response> sendNotification(@Valid NotificationRequest request) {
    return webhookService.sendNotification(request)
        .replaceWith(Response.accepted().build());
  }
}
