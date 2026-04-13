package it.pagopa.selfcare.onboarding.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.service.ProductAzureService;
import it.pagopa.selfcare.onboarding.model.ProductResource;
import it.pagopa.selfcare.onboarding.model.mapper.ProductMapper;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Api(tags = "product")
public class ProductController {

    private final ProductAzureService productAzureService;
    private final ProductMapper productMapper;

    public ProductController(ProductAzureService productAzureService, ProductMapper productMapper) {
        this.productAzureService = productAzureService;
        this.productMapper = productMapper;
    }

    @GET
    @Path("/v1/product/{id}")
    @Operation(summary = "${swagger.onboarding.product.api.getProduct}",
            description = "${swagger.onboarding.product.api.getProduct}", operationId = "getProductUsingGET")
    public ProductResource getProduct(@ApiParam("${swagger.onboarding.product.model.id}")
                                      @PathParam("id")
                                      String id,
                                      @ApiParam("${swagger.onboarding.institutions.model.institutionType}")
                                      @QueryParam("institutionType")
                                      Optional<InstitutionType> institutionType) {
        log.trace("getProduct start");
        log.debug("getProduct id = {}, institutionType = {}", id, institutionType);
        Product product = productAzureService.getProduct(id, institutionType.orElse(null));
        ProductResource resource = productMapper.toResource(product);
        log.debug("getProduct result = {}", resource);
        log.trace("getProduct end");
        return resource;
    }

    @GET
    @Path("/v1/products")
    @Operation(summary = "${swagger.onboarding.product.api.getProducts}",
            description = "${swagger.onboarding.product.api.getProducts}", operationId = "getProducts")
    public List<ProductResource> getProducts() {
        log.trace("getProducts start");
        final List<Product> products = productAzureService.getProducts(false);
        List<ProductResource> resources = products.stream()
                .map(productMapper::toResource)
                .toList();
        log.debug("getProducts result = {}", resources);
        log.trace("getProducts end");
        return resources;
    }

    @GET
    @Path("/v1/products/admin")
    @Operation(summary = "${swagger.onboarding.product.api.getProductsAdmin}",
            description = "${swagger.onboarding.product.api.getProductsAdmin}", operationId = "getProductsAdmin")
    public List<ProductResource> getProductsAdmin() {
        log.trace("getProductsAdmin start");
        final List<Product> products = productAzureService.getProducts(true);
        List<ProductResource> resources = products.stream()
                .filter(product -> Objects.nonNull(product.getUserContractTemplate(Product.CONTRACT_TYPE_DEFAULT).getContractTemplatePath()))
                .map(productMapper::toResource)
                .toList();
        log.debug("getProductsAdmin result = {}", resources);
        log.trace("getProductsAdmin end");
        return resources;
    }
}
