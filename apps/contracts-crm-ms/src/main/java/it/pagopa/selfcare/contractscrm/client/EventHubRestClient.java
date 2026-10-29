package it.pagopa.selfcare.contractscrm.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.client.auth.EventhubTokenAuthorization;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "event-hub")
@RegisterProvider(EventhubTokenAuthorization.class)
@Path("/")
public interface EventHubRestClient {

  @POST
  @Path("messages")
  Uni<Void> sendMessage(ContractDelegationResponse contractDelegation);
}
