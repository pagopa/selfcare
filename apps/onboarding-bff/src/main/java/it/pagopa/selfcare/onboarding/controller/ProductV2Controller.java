package it.pagopa.selfcare.onboarding.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.model.OriginResponse;
import it.pagopa.selfcare.onboarding.model.mapper.ProductMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@Path("/v2/product")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "product-ms")
public class ProductV2Controller {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductV2Controller(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GET
    @Operation(summary = "${swagger.product.ms.api.getOrigins.summary}",
            description = "${swagger.product.ms.api.getOrigins.description}", operationId = "getOrigins")
    public OriginResponse getOrigins(@Parameter(description = "${swagger.onboarding.institutions.model.institutionType}")
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

}
