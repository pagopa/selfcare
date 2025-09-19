package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.service.SAMLService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
  public Uni<Response> handleSamlResponse(@Context ContainerRequestContext requestContext, @FormParam("SAMLResponse") String samlResponse) throws Exception {
    log.info("{}", samlResponse == null ? null : samlResponse.replaceAll("[\\r\\n]", ""));
    MediaType contentType = requestContext.getMediaType();
    if (contentType == null || !MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(contentType)) {
      return Uni.createFrom().item(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
        .entity("Unsupported Content-Type. Expected: " + MediaType.APPLICATION_FORM_URLENCODED)
        .build());
    }

    if (samlResponse == null || samlResponse.trim().isEmpty()) {
      return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
        .entity("SAMLResponse is required.")
        .build());
    }

    return samlService.validate(samlResponse)
      .onItem().transformToUni(isValid -> Uni.createFrom().item(isValid ? createResponse(samlResponse) : createErrorResponse()));
  }

  Response createResponse(String message) {
    byte[] response = Base64.getDecoder().decode(message.getBytes(StandardCharsets.UTF_8));
    String responseMessage = String.format("samlResponse: %s", new String(response));

    return Response.ok(responseMessage).build();
  }

  Response createErrorResponse() {
    return Response.status(Response.Status.BAD_REQUEST)
      .entity("SAMLResponse not valid")
      .build();
  }
}
