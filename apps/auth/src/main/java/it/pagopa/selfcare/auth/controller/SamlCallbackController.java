package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
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
import java.util.Objects;
import java.util.Optional;

@Tag(name = "SAML")
@Path("/saml")
@RequiredArgsConstructor
@Slf4j
public class SamlCallbackController {

  private final SAMLService samlService;

  enum ValidationResult {
    VALID, INVALID_CONTENT_TYPE, MISSING_SAML_RESPONSE
  }

  @POST
  @Path("/acs")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public Uni<Response> handleSamlResponse(@Context ContainerRequestContext requestContext,
                                          @FormParam("SAMLResponse") String samlResponse) throws Exception {
    log.info("{}", samlResponse == null ? null : samlResponse.replaceAll("[\\r\\n]", ""));

    return validateRequest(requestContext.getMediaType(), samlResponse)
      .onItem().transformToUni(validationResult -> switch (validationResult) {
        case INVALID_CONTENT_TYPE -> Uni.createFrom().item(
          Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
            .entity("Unsupported Content-Type. Expected: " + MediaType.APPLICATION_FORM_URLENCODED)
            .build());
        case MISSING_SAML_RESPONSE -> Uni.createFrom().item(
          Response.status(Response.Status.BAD_REQUEST)
            .entity("SAMLResponse is required.")
            .build());
        case VALID -> {
          try {
            yield samlService.generateSessionToken(samlResponse)
              .onItem().transform(message -> createResponse(Objects.requireNonNull(message)));
          } catch (Exception e) {
            throw new SamlSignatureException(e.getMessage());
//            createErrorResponse();
          }
        }
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

  private Uni<ValidationResult> validateRequest(MediaType contentType, String samlResponse) {
    return Uni.createFrom().item(() -> Optional.ofNullable(contentType)
      .filter(MediaType.APPLICATION_FORM_URLENCODED_TYPE::isCompatible)
      .map(ct -> Optional.ofNullable(samlResponse)
        .filter(sr -> !sr.trim().isEmpty())
        .map(sr -> ValidationResult.VALID)
        .orElse(ValidationResult.MISSING_SAML_RESPONSE))
      .orElse(ValidationResult.INVALID_CONTENT_TYPE));
  }
}
