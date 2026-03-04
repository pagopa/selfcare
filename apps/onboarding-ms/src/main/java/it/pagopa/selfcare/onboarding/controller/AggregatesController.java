package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.service.AggregatesService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;

@Authenticated
@Path("/v1/aggregates")
@Tag(name = "Aggregates Controller")
@AllArgsConstructor
public class AggregatesController {

    @Inject
    AggregatesService aggregatesService;

    @Operation(
            summary = "Validate the data related to the aggregated entities present in a CSV file",
            description = "Validates aggregated entity data specific to the PROD-IO environment by processing the provided CSV file. This ensures that all entries meet the required criteria before further processing."
    )
    @POST
    @Path("/verification/prod-io")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifyAppIoAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validateAppIoAggregatesCsv(file);
    }

    @Operation(
            summary = "Validate the data related to the aggregated entities present in a CSV file",
            description = "Validates aggregated entity data specific to the PROD-Pagopa environment by processing the provided CSV file. This ensures that all entries meet the required criteria before further processing."
    )
    @POST
    @Path("/verification/prod-pagopa")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifyPagoPaAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validatePagoPaAggregatesCsv(file);
    }

    @Operation(
            summary = "Validate the data related to the aggregated entities present in a CSV file",
            description = "Validates aggregated entity data specific to the PROD-PN environment by processing the provided CSV file. This ensures that all entries meet the required criteria before further processing."
    )
    @POST
    @Path("/verification/prod-pn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifySendAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validateSendAggregatesCsv(file);
    }

    @Operation(
            summary = "Retrieve aggregates csv for a given onboarding and product",
            description = "Downloads the aggregates csv associated with the specified onboarding ID and product."
    )
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/csv/{onboardingId}/products/{productId}")
    public Uni<RestResponse<File>> getAggregatesCsv(@PathParam(value = "onboardingId") String onboardingId,
                                                    @PathParam(value = "productId") String productId){
        return aggregatesService.retrieveAggregatesCsv(onboardingId, productId);
    }


}
