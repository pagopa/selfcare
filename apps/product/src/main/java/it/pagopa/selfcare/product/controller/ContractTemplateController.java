package it.pagopa.selfcare.product.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.dto.response.Problem;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import it.pagopa.selfcare.product.service.ContractTemplateService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Authenticated
@Path("/contract-template")
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    public ContractTemplateController(ContractTemplateService contractTemplateService) {
        this.contractTemplateService = contractTemplateService;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload a new contract template version", description = "Upload an html file as a new contract template version to be used in product configuration")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContractTemplateResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    })
    public Uni<Response> upload(@Valid ContractTemplateUploadRequest uploadRequest) {
        return contractTemplateService.upload(uploadRequest)
                .onItem().transform(r -> Response.status(Response.Status.CREATED).entity(r).build());
    }

    @GET
    @Path("/{contractTemplateId}")
    @Operation(summary = "Download a contract template version", description = "Download the html file of a specific contract template version")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    })
    public Uni<Response> download(@QueryParam("productId") String productId,
                                  @QueryParam("fileType") @DefaultValue("HTML") String fileType,
                                  @PathParam("contractTemplateId") String contractTemplateId) {
        return contractTemplateService.download(productId, contractTemplateId, ContractTemplateFileType.from(fileType))
                .onItem().transform(r -> Response.ok(r.getData()).type(r.getType().getContentType()).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List contract templates available", description = "List and filter all the contract templates available")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    })
    public Uni<Response> list(@QueryParam("productId") String productId,
                              @QueryParam("name") String name,
                              @QueryParam("version") String version) {
        return contractTemplateService.list(productId, name, version)
                .onItem().transform(r -> Response.ok(r).build());
    }

}
