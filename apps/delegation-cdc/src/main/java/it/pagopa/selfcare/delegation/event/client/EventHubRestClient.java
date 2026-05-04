package it.pagopa.selfcare.delegation.event.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.auth.EventhubSasTokenAuthorization;
import it.pagopa.selfcare.delegation.event.model.DelegationNotificationToSend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "event-hub")
@ApplicationScoped
@Path("/")
@RegisterProvider(EventhubSasTokenAuthorization.class)
public interface EventHubRestClient {

    @POST
    @Path("messages")
    Uni<Void> sendMessage(DelegationNotificationToSend notification);

}
