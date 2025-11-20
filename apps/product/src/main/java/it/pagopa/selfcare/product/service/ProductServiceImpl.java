package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.util.JsonUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.owasp.encoder.Encode;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProductServiceImpl implements ProductService {

    //JPA
    private final ProductRepository productRepository;

    //MAPPER
    private final ProductMapperRequest productMapperRequest;
    private final ProductMapperResponse productMapperResponse;

    //VALIDATOR
    //private final UserValidator userValidator;

    //UTILS
    private final JsonUtils jsonUtils;

    @Override
    public Uni<String> ping() {
        return Uni.createFrom().item("OK");
    }

    @Override
    public Uni<ProductBaseResponse> createProduct(ProductCreateRequest productCreateRequest) {
        Product requestProduct = productMapperRequest.toProduct(productCreateRequest);

        String productId = requestProduct.getProductId();

        if (StringUtils.isBlank(productId)) {
            throw new BadRequestException(String.format("Missing product by id: %s", productId));
        }

        if (requestProduct.getStatus() == null) {
            log.info("Product status missing - default TESTING");
            requestProduct.setStatus(ProductStatus.TESTING);
        }

        requestProduct.setCreatedAt(Instant.now());

        return productRepository.findProductById(productId).onItem().ifNotNull().transformToUni(currentProduct -> {
                    int nextVersion = currentProduct.getVersion() + 1;
                    requestProduct.setVersion(nextVersion);
                    log.info("Updating configuration of product {} with version {}", productId, nextVersion);
                    return productRepository.persist(productMapperRequest.cloneObject(currentProduct, requestProduct)).replaceWith(requestProduct);
                }).onItem().ifNull().switchTo(() -> {
                    log.info("Adding new config of product {}", productId);
                    return productRepository.persist(requestProduct).replaceWith(requestProduct);
                })
                .map(productUpdated -> productMapperResponse.toProductBaseResponse(
                        Product.builder().id(productUpdated.getId()).productId(productUpdated.getProductId()).status(productUpdated.getStatus()).build()
                ));
    }

    @Override
    public Uni<ProductResponse> getProductById(String productId) {
        String sanitizedProductId = Encode.forJava(productId);
        log.info("Getting info from product {}", sanitizedProductId);
        if (StringUtils.isBlank(sanitizedProductId)) {
            return Uni.createFrom().failure(new IllegalArgumentException(String.format("Missing product by productId: %s", sanitizedProductId)));
        }

        return productRepository.findProductById(sanitizedProductId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + sanitizedProductId + " not found"))
                .map(productMapperResponse::toProductResponse);
    }

    @Override
    public Uni<ProductBaseResponse> deleteProductById(String productId) {
        String sanitizedProductId = Encode.forJava(productId);
        log.info("Delete product configuration by productId: {}", sanitizedProductId);

        if (StringUtils.isBlank(sanitizedProductId)) {
            return Uni.createFrom().failure(new IllegalArgumentException(String.format("Missing product by productId: %s", sanitizedProductId)));
        }

        return productRepository.findProductById(sanitizedProductId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + sanitizedProductId + " not found"))
                .invoke(currentProduct -> currentProduct.setStatus(ProductStatus.DELETED))
                .call(productRepository::update)
                .map(productMapperResponse::toProductBaseResponse);
    }

    @Override
    public Uni<ProductResponse> patchProductById(String productId, JsonValue body) {
        String sanitizedProductId = Encode.forJava(productId);
        log.info("Update product configuration by productId: {}", sanitizedProductId);
        return Uni.createFrom().item(() -> {
                    if (StringUtils.isBlank(productId)) {
                        throw new BadRequestException("Missing productId");
                    }
                    return jsonUtils.toMergePatch(body);
                })
                .onItem().transformToUni(patch ->
                        productRepository.findProductById(productId)
                                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + productId + " not found"))
                                .onItem().transformToUni(current -> {
                                    Product candidate = jsonUtils.mergePatch(patch, current, Product.class);
                                    candidate.setId(UUID.randomUUID().toString());
                                    candidate.setProductId(current.getProductId());
                                    candidate.setCreatedAt(Instant.now());
                                    candidate.setVersion(current.getVersion() + 1);
                                    return productRepository.persist(candidate)
                                            .map(productMapperResponse::toProductResponse);
                                })
                );
    }

}