package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.controller.response.OriginResponse;
import it.pagopa.selfcare.onboarding.controller.response.RequiredDocumentsEnabledResource;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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

    private final ProductService productService;
    private final InstitutionMapper productMapper;

    @GET
    @Operation(summary = "${openapi.product.ms.api.getOrigins.summary}",
            description = "${openapi.product.ms.api.getOrigins.description}", operationId = "getOrigins")
    public OriginResponse getOrigins(@Parameter(description = "${openapi.onboarding.institutions.model.institutionType}")
                                      @QueryParam("productId")
                                      String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        log.debug("getOrigins productId = {}", productIdSanitized);
        OriginResult originEntries = productService.getOrigins(productId);
        OriginResponse response = productMapper.toOriginResponse(originEntries);
        log.trace("getOrigins end");
        return response;
    }

    @GET
    @Path("/{productId}/required-documents")
    @Operation(summary = "Get required documents for a product",
            description = "Returns the list of required documents for the given product, institutionType and origin.",
            operationId = "getRequiredDocuments")
    public List<RequiredDocumentModel> getRequiredDocuments(@PathParam("productId") String productId,
                                                            @QueryParam("institutionType") String institutionType,
                                                            @QueryParam("origin") String origin) {
        log.trace("getRequiredDocuments start");
        log.debug("getRequiredDocuments productId = {}, institutionType = {}, origin = {}",
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin));
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(productId, institutionType, origin);
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @GET
    @Path("/{productId}/required-documents/enabled")
    @Operation(summary = "Check if required documents are enabled for a product",
            description = "Returns an object with the boolean flag requiredDocumentsEnabled = true when required documents are configured for the given product, institutionType and origin.",
            operationId = "isRequiredDocumentsEnabled")
    public RequiredDocumentsEnabledResource isRequiredDocumentsEnabled(@PathParam("productId") String productId,
                                                                       @QueryParam("institutionType") String institutionType,
                                                                       @QueryParam("origin") String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        log.debug("isRequiredDocumentsEnabled productId = {}, institutionType = {}, origin = {}",
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin));
        boolean result = productService.isRequiredDocumentsEnabled(productId, institutionType, origin);
        log.debug("isRequiredDocumentsEnabled result = {}", result);
        log.trace("isRequiredDocumentsEnabled end");
        return new RequiredDocumentsEnabledResource(result);
    }

}
