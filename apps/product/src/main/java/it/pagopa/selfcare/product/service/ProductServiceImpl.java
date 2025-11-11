package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.controller.request.ProductCreateRequest;
import it.pagopa.selfcare.product.controller.response.ProductBaseResponse;
import it.pagopa.selfcare.product.controller.response.ProductResponse;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.validator.entity.UserValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;

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
        Product product = productMapperRequest.toProduct(productCreateRequest);

        if (StringUtils.isBlank(product.getAlias())) {
            throw new BadRequestException(String.format("Missing product by id: %s", product.getAlias()));
        }

        if (StringUtils.isBlank(product.getStatus().name())) {
            log.info("Product status missing - default TESTING");
            product.setStatus(ProductStatus.TESTING);
        }

        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        product.setVersion(1);

        product.setId(UUID.randomUUID().toString());

        return productRepository.persist(product)
                .replaceWith(product)
                .map(storedProduct -> productMapperResponse.toProductBaseResponse(
                        Product.builder().id(storedProduct.getId()).productId(storedProduct.getProductId()).status(storedProduct.getStatus()).build()
                ));
    }

    @Override
    public Uni<ProductResponse> getProductById(String id) {
        if (StringUtils.isBlank(id)) {
            return Uni.createFrom().failure(new IllegalArgumentException(String.format("Missing product by id: %s", id)));
        }
        return productRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Product " + id + " not found"))
                .map(productMapperResponse::toProductResponse);
    }
}
