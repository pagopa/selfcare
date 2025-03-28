package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.request.OidcExchangeRequest;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.controller.response.Problem;
import it.pagopa.selfcare.auth.service.OidcService;
import jakarta.validation.Valid;
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

@Tag(name = "OIDC")
@Path("/oidc")
@RequiredArgsConstructor
@Slf4j
public class OidcController {

    private final OidcService oidcService;

    @Operation(
            description = "OIDC exchange endpoint provides a token exchange by accepting a valid authorization code and releasing a jwt session token",
            summary = "OIDC exchange endpoint",
            operationId = "oidcExchange"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = OidcExchangeRequest.class), mediaType = "application/json")),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @POST
    @Path(value = "/exchange")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OidcExchangeResponse> oidcExchange(@Valid OidcExchangeRequest oidcExchangeRequest) {
        return oidcService.exchange(oidcExchangeRequest.code, oidcExchangeRequest.redirectUri);
    }

}

