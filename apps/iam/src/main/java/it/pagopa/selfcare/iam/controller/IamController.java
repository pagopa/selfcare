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
      .onItem().transform(user -> Response.ok(user).build());
//      .onFailure(IllegalArgumentException.class)
//      .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
//        .entity(Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build())
//        .build())
//      .onFailure()
//      .recoverWithItem(ex -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//        .entity(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build())
//        .build());
  }

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
//      .onFailure(IllegalArgumentException.class)
//      .recoverWithItem(ex -> Response.status(Response.Status.BAD_REQUEST)
//        .entity(Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build())
//        .build())
//      .onFailure(ResourceNotFoundException.class)
//      .recoverWithItem(ex -> Response.status(Response.Status.NOT_FOUND)
//        .entity(Response.status(Response.Status.NOT_FOUND).entity(ex.getMessage()).build())
//        .build())
//      .onFailure()
//      .recoverWithItem(ex -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//        .entity(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build())
//        .build());
  }

}

