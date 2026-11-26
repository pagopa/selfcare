package it.pagopa.selfcare.user.client;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTO;
import org.openapi.quarkus.one_mail_json.model.EmailSuccessResponseDTO;

@ApplicationScoped
@RegisterRestClient(configKey = "one-mail")
@Path("/v1/emails")
public interface OneMailClient {

  @POST
  @Path("/send/high")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Uni<EmailSuccessResponseDTO> sendHighPriority(
      @HeaderParam("x-api-key") String apiKey,
      @QueryParam("dryRun") boolean dryRun,
      EmailHighPriorityBodyDTO request);
}
