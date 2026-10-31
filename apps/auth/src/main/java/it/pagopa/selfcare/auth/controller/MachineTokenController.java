package it.pagopa.selfcare.auth.controller;

import io.smallrye.common.annotation.Blocking;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.service.MachineTokenService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/token/service")
@Tag(name = "Machine authentication")
@SecurityScheme(
    securitySchemeName = "entraMachineToken",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Entra v2 app-only access token issued for auth")
@Blocking
public class MachineTokenController {
  @Inject MachineTokenService service;

  @POST
  @PermitAll // This endpoint validates Entra credentials, not SelfCare user sessions.
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Issue a machine access token",
      description =
          "Exchange an Entra app-only access token for a tenant-bound, authorized service token.",
      operationId = "issueMachineToken")
  @SecurityRequirement(name = "entraMachineToken")
  @APIResponse(
      responseCode = "200",
      description = "Machine access token",
      content = @Content(schema = @Schema(implementation = MachineTokenResponse.class)))
  @APIResponse(responseCode = "400", description = "Invalid request")
  @APIResponse(responseCode = "401", description = "Invalid Entra credential")
  @APIResponse(responseCode = "403", description = "Machine identity is not authorized")
  @APIResponse(responseCode = "404", description = "Machine token issuance is disabled")
  public Response issueMachineToken(
      @HeaderParam("Authorization") String authorization,
      @NotNull @Valid MachineTokenRequest request) {
    return Response.ok(service.issue(authorization, request))
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .build();
  }
}
