package it.pagopa.selfcare.user.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.auth.EventhubFdSasTokenAuthorization;
import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "event-hub-fd")
@ApplicationScoped
@Path("/")
@RegisterProvider(EventhubFdSasTokenAuthorization.class)
public interface EventHubFdRestClient {

    @POST
    @Path("messages")
    Uni<Void> sendMessage(FdUserNotificationToSend notification);

}
