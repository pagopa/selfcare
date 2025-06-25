package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.*;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.openapi.quarkus.internal_json.model.UserResource;

@Tag(name = "OTP")
@Path("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(
            description = "userInfo",
            summary = "userInfo",
            operationId = "userInfo"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @GET
    @Path(value = "/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> userInfo() {
        return userService.getUserInfoEmail(UserClaims.builder().fiscalCode("FRRGPR80A01C573J").build());
    }

}

