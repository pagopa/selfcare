package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.util.ProductUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.owasp.encoder.Encode;

import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProductServiceImpl implements ProductService {

    public static final String GETTING_INFO_FROM_PRODUCT = "Getting info from product {}";

    //JPA
    private final ProductRepository productRepository;

    //MAPPER
    private final ProductMapperRequest productMapperRequest;
    private final ProductMapperResponse productMapperResponse;

    //UTILS
    private final ProductUtils productUtils;

    @Override
    public Uni<String> ping() {
        return Uni.createFrom().item("OK");
    }

    @Override
    public Uni<ProductBaseResponse> createProduct(String productId, String createdBy, ProductCreateRequest productCreateRequest) {

        if (StringUtils.isBlank(productId)) {
            throw new BadRequestException(String.format("Invalid productId: %s", productId));
        }

        String sanitizedProductId = Encode.forJava(productId);
        String sanitizedCreatedBy = Encode.forJava(createdBy);

        log.info("Adding product {} action by {}", sanitizedProductId, sanitizedCreatedBy);

        Product requestProduct = productMapperRequest.toProduct(productCreateRequest);
        requestProduct.setProductId(productId);

        if (requestProduct.getStatus() == null) {
            log.info("Product status missing - default TESTING");
            requestProduct.setStatus(ProductStatus.TESTING);
        }

        requestProduct.setMetadata(productUtils.buildProductMetadata(createdBy));

        return productRepository.findProductById(productId)
                .onItem().ifNotNull().transformToUni(currentProduct -> {
                    int nextVersion = currentProduct.getVersion() + 1;
                    requestProduct.setVersion(nextVersion);
                    log.info("Updating configuration of product {} with version {}", sanitizedProductId, nextVersion);
                    return productRepository.persist(productMapperRequest.cloneObject(currentProduct, requestProduct))
                            .replaceWith(requestProduct);
                }).onItem().ifNull().switchTo(() -> {
                    log.info("Adding new config of product {}", sanitizedProductId);
                    return productRepository.persist(requestProduct).replaceWith(requestProduct);
                })
                .map(productUpdated -> productMapperResponse.toProductBaseResponse(
                        Product.builder()
                                .id(productUpdated.getId())
                                .productId(productUpdated.getProductId())
                                .status(productUpdated.getStatus())
                                .build()
                ));
    }

    public Uni<ProductResponse> getProductById(String productId) {
        if (StringUtils.isBlank(productId)) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(String.format("Missing product by productId: %s", productId))
            );
        }

        String sanitizedProductId = Encode.forJava(productId);
        log.info(GETTING_INFO_FROM_PRODUCT, sanitizedProductId);

        return productRepository.findProductById(productId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + productId + " not found"))
                .map(productMapperResponse::toProductResponse);
    }

    @Override
    public Uni<ProductBaseResponse> deleteProductById(String productId) {
        if (StringUtils.isBlank(productId)) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(String.format("Missing product by productId: %s", productId))
            );
        }

        String sanitizedProductId = Encode.forJava(productId);
        log.info("Delete product configuration by productId: {}", sanitizedProductId);

        return productRepository.findProductById(sanitizedProductId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + sanitizedProductId + " not found"))
                .invoke(currentProduct -> currentProduct.setStatus(ProductStatus.DELETED))
                .call(productRepository::update)
                .map(productMapperResponse::toProductBaseResponse);
    }

    @Override
    public Uni<ProductResponse> patchProductById(String productId, String createdBy, ProductPatchRequest productPatchRequest) {
        String sanitizedProductId = Encode.forJava(productId);
        String sanitizedCreatedBy = Encode.forJava(createdBy);
        log.info("Update product configuration by productId: {} by user {}", sanitizedProductId, sanitizedCreatedBy);

        return Uni.createFrom().item(() -> {
                    if (StringUtils.isBlank(productId)) {
                        throw new BadRequestException("Missing productId");
                    }
                    if (productPatchRequest == null) {
                        throw new BadRequestException("Missing request patch object into body");
                    }
                    return productPatchRequest;
                })
                .onItem().transformToUni(patchRequest ->
                        productRepository.findProductById(productId)
                                .onItem().ifNull().failWith(() ->
                                        new NotFoundException("Product " + productId + " not found"))
                                .onItem().transformToUni(current -> {

                                    current = productMapperRequest.toPatch(patchRequest, current);

                                    current.setId(UUID.randomUUID().toString());
                                    current.setProductId(current.getProductId());
                                    current.setMetadata(productUtils.buildProductMetadata(createdBy));
                                    current.setVersion(current.getVersion() + 1);

                                    return productRepository.persist(current)
                                            .map(productMapperResponse::toProductResponse);
                                })
                );
    }

    @Override
    public Uni<ProductOriginResponse> getProductOriginsById(String productId) {
        if (StringUtils.isBlank(productId)) {
            return Uni.createFrom().failure(new IllegalArgumentException(String.format("Missing product by productId: %s", productId)));
        }

        String sanitizedProductId = Encode.forJava(productId);
        log.info(GETTING_INFO_FROM_PRODUCT, sanitizedProductId);

        return productRepository.findProductById(productId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + sanitizedProductId + " not found"))
                .map(productMapperResponse::toProductOriginResponse);
    }

}