package it.pagopa.selfcare.product.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.Problem;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Tag(name = "Product")
@Path("/product")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class ProductController {

    // SERVICE
    private final ProductService productService;

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
        return productService.ping();
    }

    @POST
    @Tag(name = "Product")
    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Operation(summary = "Create or update a product configuration", description = "Creates a new product configuration or updates the existing one when a match is found")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductBaseResponse.class))),
            @APIResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class))),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createProduct(
            @QueryParam("createdBy") String createdBy,
            @Valid ProductCreateRequest productCreateRequest) {
        return productService.createProduct(productCreateRequest, createdBy)
                .onItem().transform(productResponse ->
                        Response.status(Response.Status.CREATED)
                                .entity(productResponse)
                                .build());
    }

    @GET
    @Path("/{productId}")
    @Tag(name = "Product")
    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Operation(
            summary = "Get product by productId",
            description = "Retrieve a product by its unique identifier.",
            operationId = "getProductById"
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Product found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ProductResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            )
    })
    public Uni<Response> getProductById(
           @PathParam("productId") String productId) {
        return productService.getProductById(productId)
                .onItem().transform(product ->
                        Response.ok(product).build()
                )
                .onFailure(NotFoundException.class).recoverWithItem(() ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Problem.builder()
                                        .title("Product not found")
                                        .detail("No product found with productId=" + productId)
                                        .status(Response.Status.NOT_FOUND.getStatusCode())
                                        .instance("/products/" + productId)
                                        .build())
                                .build()
                );
    }

    @DELETE
    @Path("/{productId}")
    @Tag(name = "Product")
    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Cancel product by ID",
            description = "Marks the product as DELETED if it exists.",
            operationId = "deleteProductById"
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Product cancelled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ProductBaseResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class))
            )
    })
    public Uni<Response> deleteProductById(
            @PathParam("productId") String productId) {
        return productService.deleteProductById(productId)
                .map(product -> Response.ok(product).build())
                .onFailure(IllegalArgumentException.class).recoverWithItem(() ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(Problem.builder()
                                        .title("Invalid productId")
                                        .detail("productId is required and must be non-blank")
                                        .status(Response.Status.BAD_REQUEST.getStatusCode())
                                        .instance("/products/" + productId)
                                        .build())
                                .build()
                )
                .onFailure(NotFoundException.class).recoverWithItem(() ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Problem.builder()
                                        .title("Product not found")
                                        .detail("No product found with productId=" + productId)
                                        .status(Response.Status.NOT_FOUND.getStatusCode())
                                        .instance("/products/" + productId)
                                        .build())
                                .build()
                );
    }

    @PATCH
    @Path("/{productId}")
    @Tag(name = "Product")
    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Operation(
            summary = "Partially update a product by ID",
            description = "Partially updates an existing product identified by its productId. "
                    + "Only the non-null fields provided in the request body will be updated; "
                    + "all other fields will remain unchanged."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Product successfully patched",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ProductBaseResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid patch document, invalid productId, or field constraint violation",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Not Found - No product found with the specified productId",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class)
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unexpected error occurred while applying the patch",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class)
                    )
            )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> patchProductById(
            @PathParam("productId") String productId, @QueryParam("createdBy") String createdBy,
            ProductPatchRequest productPatchRequest) {

        return productService.patchProductById(productId, createdBy, productPatchRequest)
                .map(updated -> Response.ok(updated).build())
                .onFailure(IllegalArgumentException.class).recoverWithItem(() ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .type("application/problem+json")
                                .entity(Problem.builder()
                                        .title("Invalid productId")
                                        .detail("productId is required and must be non-blank")
                                        .status(400)
                                        .instance("/products/" + productId)
                                        .build())
                                .build())
                .onFailure(BadRequestException.class).recoverWithItem(t -> {
                    log.error("Unexpected error occurred while while parsing data for {}", productId, t);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .type("application/problem+json")
                            .entity(Problem.builder()
                                    .title("Bad Request")
                                    .detail("Invalid patch payload or field constraints violated")
                                    .status(400)
                                    .instance("/products/" + productId)
                                    .build())
                            .build();
                })
                .onFailure(NotFoundException.class).recoverWithItem(() ->
                        Response.status(Response.Status.NOT_FOUND)
                                .type("application/problem+json")
                                .entity(Problem.builder()
                                        .title("Product not found")
                                        .detail("No product found with productId=" + productId)
                                        .status(404)
                                        .instance("/products/" + productId)
                                        .build())
                                .build())
                .onFailure().recoverWithItem(t -> {
                    log.error("Unexpected error occurred while patching product with id {}", productId, t);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .type("application/problem+json")
                            .entity(Problem.builder()
                                    .title("Internal Server Error")
                                    .detail("An unexpected error occurred.")
                                    .status(500)
                                    .instance("/products/" + productId)
                                    .build())
                            .build();
                });
    }

    @GET
    @Tag(name = "Product")
    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Path("/origins")
    @Operation(
            summary = "Get product origins by productId",
            description = "Retrieve the list of institution origins for the given product.",
            operationId = "getProductOriginsById"
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Origins found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ProductOriginResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class)
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = Problem.class)
                    )
            )
    })
    public Uni<Response> getProductOriginsById(
            @Parameter(
                    name = "productId",
                    required = true
            )
            @QueryParam("productId") String productId) {
        return productService.getProductOriginsById(productId)
                .onItem().transform(originsResponse ->
                        Response.ok(originsResponse).build()
                )
                .onFailure(NotFoundException.class).recoverWithItem(() ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Problem.builder()
                                        .title("Product not found")
                                        .detail("No product found with productId=" + productId)
                                        .status(Response.Status.NOT_FOUND.getStatusCode())
                                        .instance("/products/" + productId + "/origins")
                                        .build())
                                .build()
                );
    }

}