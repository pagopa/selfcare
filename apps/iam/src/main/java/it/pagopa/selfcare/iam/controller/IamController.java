package it.pagopa.selfcare.iam.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.response.*;
import it.pagopa.selfcare.iam.service.IamService;
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


}

