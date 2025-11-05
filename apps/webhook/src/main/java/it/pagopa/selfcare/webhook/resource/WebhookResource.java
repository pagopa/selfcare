package it.pagopa.selfcare.webhook.resource;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.service.WebhookService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Webhook Management", description = "API for webhook configuration management")
public class WebhookResource {
    
    @Inject
    WebhookService webhookService;
    
    @POST
    @Operation(summary = "Create a new webhook", description = "Create a new webhook configuration")
    public Uni<Response> createWebhook(@Valid WebhookRequest request) {
        return webhookService.createWebhook(request)
                .map(response -> Response.status(Response.Status.CREATED).entity(response).build());
    }
    
    @GET
    @Operation(summary = "List all webhooks", description = "Retrieve all webhook configurations")
    public Uni<List<WebhookResponse>> listWebhooks() {
        return webhookService.listWebhooks();
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Get webhook by ID", description = "Retrieve a specific webhook configuration")
    public Uni<Response> getWebhook(@PathParam("id") String id) {
        return webhookService.getWebhook(id)
                .map(response -> response != null 
                        ? Response.ok(response).build() 
                        : Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update webhook", description = "Update an existing webhook configuration")
    public Uni<Response> updateWebhook(@PathParam("id") String id, @Valid WebhookRequest request) {
        return webhookService.updateWebhook(id, request)
                .map(response -> Response.ok(response).build())
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete webhook", description = "Delete a webhook configuration")
    public Uni<Response> deleteWebhook(@PathParam("id") String id) {
        return webhookService.deleteWebhook(id)
                .map(deleted -> Response.noContent().build())
                .onFailure(IllegalArgumentException.class)
                .recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/notify")
    @Operation(summary = "Send notification", description = "Create and send a webhook notification")
    public Uni<Response> sendNotification(@Valid NotificationRequest request) {
        return webhookService.sendNotification(request)
                .replaceWith(Response.accepted().build());
    }
}
