package it.pagopa.selfcare.onboarding.client.webhook;

import it.pagopa.selfcare.onboarding.dto.webhook.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "webhook-client")
@ApplicationScoped
@Path("/")
public interface WebhookRestClient {

    @POST
    @Path("webhooks/notify")
    void sendNotification(NotificationRequest notificationRequest);
}
