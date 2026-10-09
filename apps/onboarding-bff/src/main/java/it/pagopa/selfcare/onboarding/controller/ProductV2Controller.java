package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.model.dto.response.OriginResponse;
import it.pagopa.selfcare.onboarding.model.dto.response.RequiredDocumentsEnabledResource;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.util.RequestParams;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

import java.util.List;

@Slf4j
@ApplicationScoped
@Authenticated
@Path("/v2/product")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "product-ms")
@RequiredArgsConstructor
public class ProductV2Controller {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final ProductService productService;
    private final InstitutionMapper productMapper;

    @Context
    UriInfo uriInfo;

    @GET
    @Operation(summary = "${openapi.product.ms.api.getOrigins.summary}",
            description = "${openapi.product.ms.api.getOrigins.description}", operationId = "getOrigins")
    public Uni<OriginResponse> getOrigins(@Parameter(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
                                      @QueryParam("productId")
                                      String productId,
                                      @Parameter(hidden = true) @HeaderParam(TENANT_HEADER)
                                      String tenantHeader) {
        productId = RequestParams.stringQuery(uriInfo, "productId", productId);
        log.trace("getOrigins start");
        RequestParams.requiredQuery("productId", productId);
        String productIdSanitized = Encode.forJava(productId);
        log.debug("getOrigins productId = {}", productIdSanitized);
        return productService.getOrigins(requiredTenantId(tenantHeader), productId)
                .map(productMapper::toOriginResponse)
                .invoke(response -> log.trace("getOrigins end"));
    }

    @GET
    @Path("/{productId}/required-documents")
    @Operation(summary = "Get required documents for a product",
            description = "Returns the list of required documents for the given product, institutionType and origin.",
            operationId = "getRequiredDocuments")
    public List<RequiredDocumentModel> getRequiredDocuments(@Parameter(description = "The product id")
                                                            @PathParam("productId") String productId,
                                                            @Parameter(required = true) @QueryParam("institutionType") String institutionType,
                                                            @Parameter(required = true) @QueryParam("origin") String origin,
                                                            @Parameter(hidden = true) @HeaderParam(TENANT_HEADER) String tenantHeader) {
        institutionType = RequestParams.stringQuery(uriInfo, "institutionType", institutionType);
        origin = RequestParams.stringQuery(uriInfo, "origin", origin);
        log.trace("getRequiredDocuments start");
        RequestParams.requiredQuery("institutionType", institutionType);
        RequestParams.requiredQuery("origin", origin);
        log.debug("getRequiredDocuments productId = {}, institutionType = {}, origin = {}",
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin));
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(
                requiredTenantId(tenantHeader), productId, institutionType, origin);
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @GET
    @Path("/{productId}/required-documents/enabled")
    @Operation(summary = "Check if required documents are enabled for a product",
            description = "Returns an object with the boolean flag requiredDocumentsEnabled = true when required documents are configured for the given product, institutionType and origin.",
            operationId = "isRequiredDocumentsEnabled")
    public Uni<RequiredDocumentsEnabledResource> isRequiredDocumentsEnabled(@Parameter(description = "The product id")
                                                                       @PathParam("productId") String productId,
                                                                       @Parameter(required = true) @QueryParam("institutionType") String institutionType,
                                                                       @Parameter(required = true) @QueryParam("origin") String origin,
                                                                       @Parameter(hidden = true) @HeaderParam(TENANT_HEADER) String tenantHeader) {
        institutionType = RequestParams.stringQuery(uriInfo, "institutionType", institutionType);
        origin = RequestParams.stringQuery(uriInfo, "origin", origin);
        log.trace("isRequiredDocumentsEnabled start");
        RequestParams.requiredQuery("institutionType", institutionType);
        RequestParams.requiredQuery("origin", origin);
        log.debug("isRequiredDocumentsEnabled productId = {}, institutionType = {}, origin = {}",
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin));
        return productService.isRequiredDocumentsEnabled(
                        requiredTenantId(tenantHeader), productId, institutionType, origin)
                .map(result -> {
                    log.debug("isRequiredDocumentsEnabled result = {}", result);
                    log.trace("isRequiredDocumentsEnabled end");
                    return new RequiredDocumentsEnabledResource(result);
                });
    }

    private static String requiredTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidRequestException("Tenant context is required");
        }
        return tenantId;
    }

}
