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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ProductServiceImpl implements ProductService {

    //JPA
    private final ProductRepository productRepository;

    //MAPPER
    private final ProductMapperRequest productMapperRequest;
    private final ProductMapperResponse productMapperResponse;

    //VALIDATOR
    //private final UserValidator userValidator;

    @Override
    public Uni<String> ping() {
        return Uni.createFrom().item("OK");
    }

    @Override
    public Uni<ProductBaseResponse> createProduct(ProductCreateRequest productCreateRequest) {
        Product requestProduct = productMapperRequest.toProduct(productCreateRequest);

        if (StringUtils.isBlank(requestProduct.getAlias())) {
            throw new BadRequestException(String.format("Missing product by id: %s", requestProduct.getAlias()));
        }

        if (requestProduct.getStatus() == null) {
            log.info("Product status missing - default TESTING");
            requestProduct.setStatus(ProductStatus.TESTING);
        }

        Instant now = Instant.now();

        requestProduct.setId(UUID.randomUUID().toString());
        requestProduct.setCreatedAt(now);
        requestProduct.setUpdatedAt(now);
        requestProduct.setVersion(1);

        String productId = requestProduct.getProductId();

        return productRepository.findProductById(productId).onItem().ifNotNull().transformToUni(currentProduct -> {
                    int nextVersion = currentProduct.getVersion() == null ? 1 : currentProduct.getVersion() + 1;
                    requestProduct.setVersion(nextVersion);
                    log.info("Updating configuration of product {} with version {}", requestProduct.getProductId(), nextVersion);
                    return productRepository.persist(productMapperRequest.cloneObject(currentProduct, requestProduct)).replaceWith(requestProduct);
                }).onItem().ifNull().switchTo(() -> {
                    log.info("Adding new config of product {}", requestProduct.getProductId());
                    return productRepository.persist(requestProduct).replaceWith(requestProduct);
                })
                .map(saved -> productMapperResponse.toProductBaseResponse(
                        Product.builder().id(saved.getId()).productId(saved.getProductId()).status(saved.getStatus()).build()
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

}