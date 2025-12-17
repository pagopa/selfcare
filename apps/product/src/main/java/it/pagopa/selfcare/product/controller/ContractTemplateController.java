package it.pagopa.selfcare.product.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponseList;
import it.pagopa.selfcare.product.model.dto.response.Problem;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import it.pagopa.selfcare.product.service.ContractTemplateService;
import it.pagopa.selfcare.product.validator.AllowedFileTypes;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.owasp.encoder.Encode;

import java.io.File;
import java.util.Optional;

@Authenticated
@Path("/contract-template")
@Tag(name = "ContractTemplate")
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    public ContractTemplateController(ContractTemplateService contractTemplateService) {
        this.contractTemplateService = contractTemplateService;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "ContractTemplate")
    @Tag(name = "external-v2")
    @Operation(
            summary = "Upload a new contract template version",
            description = "Upload an html file as a new contract template version to be used in product configuration",
            operationId = "uploadContractTemplate"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContractTemplateResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    })
    public Uni<Response> upload(@QueryParam("productId") @NotNull
                                String productId,
                                @QueryParam("name") @NotNull
                                @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "The field can only contain letters, numbers, and spaces")
                                String name,
                                @QueryParam("version") @NotNull
                                @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "The format must be X.X.X where X is a number")
                                String version,
                                @QueryParam("description")
                                String description,
                                @QueryParam("createdBy")
                                String createdBy,
                                @RestForm("file") @NotNull
                                @AllowedFileTypes(value = {AllowedFileTypes.HTML}, message = "Only static HTML files are allowed")
                                FileUpload file) {
        return contractTemplateService.upload(ContractTemplateUploadRequest.builder()
                        .productId(productId)
                        .name(name)
                        .version(version)
                        .description(description)
                        .createdBy(createdBy)
                        .file(file)
                        .build())
                .onItem().transform(r -> Response.status(Response.Status.CREATED).entity(r).build());
    }

    @GET
    @Path("/{contractTemplateId}")
    @Tag(name = "ContractTemplate")
    @Tag(name = "external-v2")
    @Operation(
            summary = "Download a contract template version",
            description = "Download the html file of a specific contract template version",
            operationId = "downloadContractTemplate"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(implementation = File.class))),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    })
    public Uni<Response> download(@QueryParam("productId") String productId,
                                  @QueryParam("fileType") @DefaultValue("HTML") String fileType,
                                  @PathParam("contractTemplateId") String contractTemplateId) {
        productId = Optional.ofNullable(productId).map(Encode::forJava).orElse(null);
        fileType = Optional.ofNullable(fileType).map(Encode::forJava).orElse(null);
        contractTemplateId = Optional.ofNullable(contractTemplateId).map(Encode::forJava).orElse(null);
        return contractTemplateService.download(productId, contractTemplateId, ContractTemplateFileType.from(fileType))
                .onItem().transform(r -> Response.ok(r.getData()).type(r.getType().getContentType()).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "ContractTemplate")
    @Tag(name = "external-v2")
    @Operation(
            summary = "List contract templates available",
            description = "List and filter all the contract templates available",
            operationId = "listContractTemplates"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContractTemplateResponseList.class))),
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
