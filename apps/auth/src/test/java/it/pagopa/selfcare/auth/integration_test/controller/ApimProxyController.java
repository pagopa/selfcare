package it.pagopa.selfcare.auth.integration_test.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.integration_test.client.TestExternalInternalUserApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.internal_json.model.SearchUserDto;
import org.openapi.quarkus.internal_json.model.UserInfoResource;

@Path("/mock-apim")
@Slf4j
@ApplicationScoped
public class ApimProxyController {

  @RestClient @Inject private TestExternalInternalUserApi testExternalInternalUserApi;

  @POST
  @Path("/users")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<UserInfoResource> apimProxyV2getUserInfoUsingGET(SearchUserDto searchUserDto) {
    return testExternalInternalUserApi.v2getUserInfoUsingGET(null, searchUserDto);
  }
}
