package it.pagopa.selfcare.onboarding.client.eventhub;

import it.pagopa.selfcare.onboarding.client.auth.EventhubSasTokenAuthorization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.concurrent.CompletionStage;

@RegisterRestClient(configKey = "event-hub")
@ApplicationScoped
@Path("/")
@RegisterProvider(EventhubSasTokenAuthorization.class)
public interface EventHubRestClient {

    @POST
    @Path("{hubName}/messages")
    Void sendMessage(@PathParam("hubName") String topicName, String notification);

}


