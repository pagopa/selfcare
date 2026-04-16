package it.pagopa.selfcare.auth.integration_test.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.integration_test.client.TestExternalInternalUserApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.internal_json.model.UserOtpEmailInfoResponse;

@Path("/mock-apim")
@Slf4j
@ApplicationScoped
public class ApimProxyController {

  @RestClient @Inject private TestExternalInternalUserApi testExternalInternalUserApi;

  @GET
  @Path("/users/{userId}/otp-info")
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<UserOtpEmailInfoResponse> apimProxygeOtpInfo(@PathParam(value = "userId") String userId) {
    return testExternalInternalUserApi.getUserOtpEmailInfo(userId);
  }
}
