package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.WorkflowRule;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.WorkflowTypeResponse;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.repository.ProductRepository;
import it.pagopa.selfcare.product.util.ProductUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProductServiceImpl implements ProductService {

  public static final String GETTING_INFO_FROM_PRODUCT = "Getting info from product {}";
  private static final String MISSING_PRODUCT_BY_ID = "Missing product by productId: %s";
  private static final String PRODUCT_NOT_FOUND = "Product %s not found";


  // JPA
  private final ProductRepository productRepository;

  // MAPPER
  private final ProductMapperRequest productMapperRequest;
  private final ProductMapperResponse productMapperResponse;

  // UTILS
  private final ProductUtils productUtils;

  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }

  @Override
  public Uni<ProductBaseResponse> createProduct(
      ProductCreateRequest productCreateRequest, String createdBy) {

    if (StringUtils.isBlank(productCreateRequest.getProductId())) {
      throw new BadRequestException(
          String.format("Invalid productId: %s", productCreateRequest.getProductId()));
    }

    String sanitizedProductId = Encode.forJava(productCreateRequest.getProductId());
    String sanitizedCreatedBy = Encode.forJava(createdBy);

    log.info("Adding product {} action by {}", sanitizedProductId, sanitizedCreatedBy);

    Product requestProduct = productMapperRequest.toProduct(productCreateRequest);
    requestProduct.setProductId(productCreateRequest.getProductId());

    if (requestProduct.getStatus() == null) {
      log.info("Product status missing - default TESTING");
      requestProduct.setStatus(ProductStatus.TESTING);
    }

    requestProduct.setMetadata(productUtils.buildProductMetadata(createdBy));

    return productRepository
        .findProductById(productCreateRequest.getProductId())
        .onItem()
        .ifNotNull()
        .transformToUni(
            currentProduct -> {
              int nextVersion = currentProduct.getVersion() + 1;
              requestProduct.setVersion(nextVersion);
              log.info(
                  "Updating configuration of product {} with version {}",
                  sanitizedProductId,
                  nextVersion);
              return productRepository
                  .persist(productMapperRequest.cloneObject(currentProduct, requestProduct))
                  .replaceWith(requestProduct);
            })
        .onItem()
        .ifNull()
        .switchTo(
            () -> {
              log.info("Adding new config of product {}", sanitizedProductId);
              return productRepository.persist(requestProduct).replaceWith(requestProduct);
            })
        .map(
            productUpdated ->
                productMapperResponse.toProductBaseResponse(
                    Product.builder()
                        .id(productUpdated.getId())
                        .productId(productUpdated.getProductId())
                        .status(productUpdated.getStatus())
                        .build()));
  }

  public Uni<ProductResponse> getProductById(String productId) {
    if (StringUtils.isBlank(productId)) {
      return Uni.createFrom()
          .failure(
              new IllegalArgumentException(
                  String.format(MISSING_PRODUCT_BY_ID, productId)));
    }

    String sanitizedProductId = Encode.forJava(productId);
    log.info(GETTING_INFO_FROM_PRODUCT, sanitizedProductId);

    return productRepository
        .findProductById(productId)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, productId)))
        .map(productMapperResponse::toProductResponse);
  }

  @Override
  public Uni<ProductBaseResponse> deleteProductById(String productId) {
    if (StringUtils.isBlank(productId)) {
      return Uni.createFrom()
          .failure(
              new IllegalArgumentException(
                  String.format(MISSING_PRODUCT_BY_ID, productId)));
    }

    String sanitizedProductId = Encode.forJava(productId);
    log.info("Delete product configuration by productId: {}", sanitizedProductId);

    return productRepository
        .findProductById(sanitizedProductId)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
        .invoke(currentProduct -> currentProduct.setStatus(ProductStatus.DELETED))
        .call(productRepository::update)
        .map(productMapperResponse::toProductBaseResponse);
  }

  @Override
  public Uni<ProductResponse> patchProductById(
      String productId, String createdBy, ProductPatchRequest productPatchRequest) {
    String sanitizedProductId = Encode.forJava(productId);
    String sanitizedCreatedBy = Encode.forJava(createdBy);
    log.info(
        "Update product configuration by productId: {} by user {}",
        sanitizedProductId,
        sanitizedCreatedBy);

    return Uni.createFrom()
        .item(
            () -> {
              if (StringUtils.isBlank(productId)) {
                throw new BadRequestException("Missing productId");
              }
              if (productPatchRequest == null) {
                throw new BadRequestException("Missing request patch object into body");
              }
              return productPatchRequest;
            })
        .onItem()
        .transformToUni(
            patchRequest ->
                productRepository
                    .findProductById(productId)
                    .onItem()
                    .ifNull()
                    .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, productId)))
                    .onItem()
                    .transformToUni(
                        current -> {
                          current = productMapperRequest.toPatch(patchRequest, current);

                          current.setId(UUID.randomUUID().toString());
                          current.setProductId(current.getProductId());
                          current.setMetadata(productUtils.buildProductMetadata(createdBy));
                          current.setVersion(current.getVersion() + 1);

                          return productRepository
                              .persist(current)
                              .map(productMapperResponse::toProductResponse);
                        }));
  }

  @Override
  public Uni<ProductOriginResponse> getProductOriginsById(String productId) {
    if (StringUtils.isBlank(productId)) {
      return Uni.createFrom()
          .failure(
              new IllegalArgumentException(
                  String.format(MISSING_PRODUCT_BY_ID, productId)));
    }

    String sanitizedProductId = Encode.forJava(productId);
    log.info(GETTING_INFO_FROM_PRODUCT, sanitizedProductId);

    return productRepository
        .findProductById(productId)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
        .map(productMapperResponse::toProductOriginResponse);
  }

  @Override
  public Uni<WorkflowTypeResponse> getWorkflowType(
      String productId, InstitutionType institutionType, Origin origin) {

    if (StringUtils.isBlank(productId)) {
      return Uni.createFrom()
          .failure(new IllegalArgumentException("Missing productId"));
    }
    if (institutionType == null) {
      return Uni.createFrom()
          .failure(new IllegalArgumentException("Missing institutionType"));
    }
    if (origin == null) {
      return Uni.createFrom()
          .failure(new IllegalArgumentException("Missing origin"));
    }

    String sanitizedProductId = Encode.forJava(productId);
    log.info("Resolving workflowType for product {}, institutionType {}, origin {}",
        sanitizedProductId, institutionType, origin);

    return productRepository
        .findProductById(productId)
        .onItem()
        .ifNull()
        .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
        .map(product -> resolveWorkflowType(product, institutionType, origin, sanitizedProductId));
  }

  private WorkflowTypeResponse resolveWorkflowType(
      Product product,
      InstitutionType institutionType,
      Origin origin,
      String sanitizedProductId) {

    if (product.getWorkflowRules() == null || product.getWorkflowRules().isEmpty()) {
      throw new NotFoundException(
          String.format("No workflowRules configured for Product %s", sanitizedProductId));
    }

    return product.getWorkflowRules().stream()
        .filter(rule -> institutionType.equals(rule.getInstitutionType()))
        .filter(rule -> origin.equals(rule.getOrigin()))
        .findFirst()
        .map(WorkflowRule::getWorkflowType)
        .map(wt -> WorkflowTypeResponse.builder().workflowType(wt).build())
        .orElseThrow(() -> new NotFoundException(
            String.format(
                "No workflowRule found for product %s, institutionType %s, origin %s",
                sanitizedProductId, institutionType, origin)));
  }
}
