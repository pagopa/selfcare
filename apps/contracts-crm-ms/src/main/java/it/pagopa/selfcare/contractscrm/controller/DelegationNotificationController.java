package it.pagopa.selfcare.contractscrm.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationWebhookRequest;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import it.pagopa.selfcare.contractscrm.model.dto.response.Problem;
import it.pagopa.selfcare.contractscrm.service.ContractDelegationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Path("/v1/delegations")
@Tag(name = "Delegation Notification")
@SecurityRequirement(name = "bearerAuth")
public class DelegationNotificationController {

  private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

  private final ContractDelegationService contractDelegationService;

  public DelegationNotificationController(ContractDelegationService contractDelegationService) {
    this.contractDelegationService = contractDelegationService;
  }

  @POST
  @Path("/notify")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Receive a delegation notification",
      description =
          "Receives an SC-Delegations notification from webhook-ms. PSP_MEMBERSHIP and"
              + " PSP_RECIPIENT delegations update the contract delegation of the delegating PSP,"
              + " which is published on SC-ContractsCRMDelegations once both membership and"
              + " recipient are known. Other delegation types are ignored.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Contract delegation updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ContractDelegationResponse.class))),
    @APIResponse(responseCode = "204", description = "Delegation type not handled"),
    @APIResponse(
        responseCode = "400",
        description = "Invalid delegation",
        content =
            @Content(
                mediaType = PROBLEM_MEDIA_TYPE,
                schema = @Schema(implementation = Problem.class))),
    @APIResponse(responseCode = "401", description = "Unauthorized"),
    @APIResponse(
        responseCode = "404",
        description = "Active PSP contract not found for the delegation institutions",
        content =
            @Content(
                mediaType = PROBLEM_MEDIA_TYPE,
                schema = @Schema(implementation = Problem.class))),
    @APIResponse(
        responseCode = "500",
        description = "Internal server error",
        content =
            @Content(
                mediaType = PROBLEM_MEDIA_TYPE,
                schema = @Schema(implementation = Problem.class)))
  })
  public Uni<Response> notifyDelegation(@NotNull @Valid DelegationWebhookRequest request) {
    return contractDelegationService
        .processDelegation(request.getPayload())
        .map(
            contractDelegation ->
                contractDelegation == null
                    ? Response.noContent().build()
                    : Response.ok(contractDelegation).build());
  }
}
