package it.pagopa.selfcare.iam.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.controller.response.*;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.service.IamService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.validation.Valid;

@Tag(name = "IAM")
@Path("/iam")
@RequiredArgsConstructor
@Slf4j
public class IamController {

  private final IamService iamService;

  @Operation(
    summary = "Ping endpoint",
    operationId = "ping"
  )
  @APIResponses(value = {
    @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
  })
  @GET
  @Path(value = "/ping")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<String> ping() {
    return iamService.ping();
  }

  /**
   * Saves or updates a user with their product-specific roles.
   * If the user doesn't exist, creates a new user with a generated UID.
   * If the user exists, updates their information and merges product roles.
   *
   * @param saveUserRequest the request containing user details and product roles
   * @param productId optional product ID to filter roles for a specific product
   * @return a Uni containing the saved or updated UserClaims
   * @throws InvalidRequestException if the request or email is null/blank
   */
  @Operation(
    description = "Saves or updates a user with their product-specific roles.",
    summary = "Saves IAM User",
    operationId = "saveIAMUser"
  )
  @APIResponses(value = {
    @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class), mediaType = "application/json")),
    @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
  })
  @PATCH
  @Path(value = "/users")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> users(@Valid SaveUserRequest saveUserRequest, @QueryParam("productId") String productId) {
    return iamService.saveUser(saveUserRequest, productId)
      .onItem().transform(user -> Response.ok(user).build())
      .onFailure(IllegalArgumentException.class)
      .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
        .entity(Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build())
        .build())
      .onFailure()
      .recoverWithItem(ex -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build())
        .build());

  }

  /**
   * Retrieves a user by their ID and product ID.
   *
   * @param userId the ID of the user
   * @param productId the ID of the product
   * @return a Uni containing the UserClaims if found
   * @throws ResourceNotFoundException if the user is not found
   */
  @Operation(
    description = "Retrieves a user by their ID and product ID.",
    summary = "Get IAM User",
    operationId = "getIAMUser"
  )
  @APIResponses(value = {
    @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class), mediaType = "application/json")),
    @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
  })
  @GET
  @Path(value = "/users/{uid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> getUser(@PathParam("uid") String userId, @QueryParam("productId") String productId) {
    return iamService.getUser(userId, productId)
      .onItem().transform(user -> Response.ok(user).build());
  }

  /**
   * Checks if a user has a specific permission for a product.
   *
   * @param userId the ID of the user
   * @param permission the permission to check
   * @param productId the ID of the product
   * @param institutionId the ID of the institution
   * @return a Uni containing true if the user has the permission, false otherwise
   */
  @Operation(
    description = "Checks if a user has a specific permission for a product.",
    summary = "Check IAM User hasPermission",
    operationId = "hasIAMUserPermission"
  )
  @APIResponses(value = {
    @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Boolean.class), mediaType = "application/json")),
    @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
    @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
  })
  @GET
  @Path(value = "/users/{uid}/permissions/{permission}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> hasPermission(@PathParam("uid") String userId,
                                     @PathParam("permission") String permission,
                                     @QueryParam("productId") String productId,
                                     @QueryParam("institutionId") String institutionId) {
    return iamService.hasPermission(userId, permission, productId, institutionId)
      .onItem().transform(hasPermission -> Response.ok(hasPermission).build());
  }
}

