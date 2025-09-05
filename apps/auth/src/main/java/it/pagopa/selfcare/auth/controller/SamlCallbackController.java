package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.service.SAMLService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "SAML")
@Path("/saml")
@RequiredArgsConstructor
@Slf4j
public class SamlCallbackController {

  private final SAMLService samlService;

  @POST
  @Path("/acs")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public Uni<Response> handleSamlResponse(@FormParam("SAMLResponse") String samlResponse) {
    log.info("{}", samlResponse);
    if (samlResponse == null) {
      return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
        .entity("SAMLResponse is required.")
        .build());
    }

    return samlService.validate(samlResponse)
      .onItem().transformToUni(isValid -> {
        return Uni.createFrom().item(isValid ? createResponse(samlResponse) : createErrorResponse());
      });
  }

  Response createResponse(String message) {
    String responseMessage = String.format("samlResponse: %s", message);
    return Response.ok(responseMessage).build();
  }

  Response createErrorResponse() {
    return Response.status(Response.Status.BAD_REQUEST)
      .entity("SAMLResponse not valid")
      .build();
  }
}
