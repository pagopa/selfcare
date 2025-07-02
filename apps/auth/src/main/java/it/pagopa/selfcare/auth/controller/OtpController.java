package it.pagopa.selfcare.auth.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.request.OtpResendRequest;
import it.pagopa.selfcare.auth.controller.request.OtpVerifyRequest;
import it.pagopa.selfcare.auth.controller.response.*;
import it.pagopa.selfcare.auth.exception.UnimplementedException;
import it.pagopa.selfcare.auth.service.OtpFlowService;
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

@Tag(name = "OTP")
@Path("/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final OtpFlowService otpFlowService;

    @Operation(
            description = "Verify endpoint is used to complete an otp flow by validating user account",
            summary = "Verify OTP endpoint",
            operationId = "otpVerify"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TokenResponse.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = OtpForbidden.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @POST
    @Path(value = "/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<TokenResponse> verifyOtp(@Valid OtpVerifyRequest otpVerifyRequest) {
        return otpFlowService.verifyOtp(otpVerifyRequest.getOtpUuid(), otpVerifyRequest.getOtp());
    }

    @Operation(
            description = "Resend an OTP if email has not been received by user",
            summary = "Resend OTP endpoint",
            operationId = "otpResend"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = OidcExchangeOtpResponse.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @POST
    @Path(value = "/resend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OidcExchangeOtpResponse> resendOtp(@Valid OtpResendRequest otpResendRequest) {
        throw new UnimplementedException("Unimplemented endpoint");
    }

}

