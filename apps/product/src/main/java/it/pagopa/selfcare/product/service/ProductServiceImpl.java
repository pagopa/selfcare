package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.mapper.ProductMapperRequest;
import it.pagopa.selfcare.product.mapper.ProductMapperResponse;
import it.pagopa.selfcare.product.model.Features;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.RequiredDocument;
import it.pagopa.selfcare.product.model.WorkflowRule;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
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

import java.util.List;
import java.util.Objects;
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
  private static final String PARENT_PRODUCT_NOT_FOUND = "Parent product %s not found";
  private static final String PARENT_CANNOT_BE_CHILD = "Parent product %s cannot be a child product";
  private static final String PARENT_ID_SELF_REFERENCE = "parentId cannot be equal to productId";


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
    applyParentOnboardingDefaults(requestProduct, true);

    return validateParentRelationship(requestProduct)
        .onItem()
        .transformToUni(
            ignored ->
                productRepository
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
                                    .build())));
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
                          Product patched = productMapperRequest.toPatch(patchRequest, current);
                          applyParentOnboardingDefaults(patched, patchRequest.getParentId() != null);

                          return validateParentRelationship(patched)
                              .onItem()
                              .transformToUni(
                                  ignored -> {
                                    patched.setId(UUID.randomUUID().toString());
                                    patched.setProductId(patched.getProductId());
                                    patched.setMetadata(
                                        productUtils.buildProductMetadata(createdBy));
                                    patched.setVersion(patched.getVersion() + 1);

                                    return productRepository
                                        .persist(patched)
                                        .map(productMapperResponse::toProductResponse);
                                  });
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

    return validateProductContext(productId, institutionType, origin)
        .onItem()
        .transformToUni(ignored -> {
          String sanitizedProductId = Encode.forJava(productId);
          log.info("Resolving workflowType for product {}, institutionType {}, origin {}",
              sanitizedProductId, institutionType, origin);

          return productRepository
              .findProductById(productId)
              .onItem()
              .ifNull()
              .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
              .map(product -> {
                WorkflowTypeResponse result = resolveWorkflowType(product, institutionType, origin, sanitizedProductId);
                log.info("Workflow type resolved - productId: {}, institutionType: {}, origin: {}, workflowType: {}",
                    sanitizedProductId, institutionType, origin, result.getWorkflowType());
                return result;
              });
        });
  }

  @Override
  public Uni<Boolean> isRequiredDocumentsEnabled(
      String productId, InstitutionType institutionType, Origin origin) {

    return validateProductContext(productId, institutionType, origin)
        .onItem()
        .transformToUni(ignored -> {
          String sanitizedProductId = Encode.forJava(productId);
          log.info("Checking if required documents are enabled for product {}, institutionType {}, origin {}",
              sanitizedProductId, institutionType, origin);

          return productRepository
              .findProductById(productId)
              .onItem()
              .ifNull()
              .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
              .map(product -> {
                boolean enabled = !filterRequiredDocumentsForContext(product, institutionType, origin).isEmpty();
                log.info("Required documents check - productId: {}, institutionType: {}, origin: {}, enabled: {}",
                    sanitizedProductId, institutionType, origin, enabled);
                return enabled;
              });
        });
  }

  @Override
  public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(
      String productId, InstitutionType institutionType, Origin origin) {

    return validateProductContext(productId, institutionType, origin)
        .onItem()
        .transformToUni(ignored -> {
          String sanitizedProductId = Encode.forJava(productId);
          log.info("Retrieving required documents for product {}, institutionType {}, origin {}",
              sanitizedProductId, institutionType, origin);

          return productRepository
              .findProductById(productId)
              .onItem()
              .ifNull()
              .failWith(() -> new NotFoundException(String.format(PRODUCT_NOT_FOUND, sanitizedProductId)))
              .map(product -> {
                List<RequiredDocumentResponse> documents =
                    filterRequiredDocumentsForContext(product, institutionType, origin).stream()
                        .map(productMapperResponse::toRequiredDocumentResponse)
                        .toList();
                log.info("Required documents retrieved - productId: {}, institutionType: {}, origin: {}, count: {}",
                    sanitizedProductId, institutionType, origin, documents.size());
                return documents;
              });
        });
  }

  private Uni<Void> validateProductContext(
      String productId, InstitutionType institutionType, Origin origin) {
    if (StringUtils.isBlank(productId)) {
      return Uni.createFrom().failure(new IllegalArgumentException("Missing productId"));
    }
    if (Objects.isNull(institutionType)) {
      return Uni.createFrom().failure(new IllegalArgumentException("Missing institutionType"));
    }
    if (Objects.isNull(origin)) {
      return Uni.createFrom().failure(new IllegalArgumentException("Missing origin"));
    }
    return Uni.createFrom().voidItem();
  }

  private List<RequiredDocument> filterRequiredDocumentsForContext(
      Product product, InstitutionType institutionType, Origin origin) {
    if (Objects.isNull(product.getRequiredDocuments()) || product.getRequiredDocuments().isEmpty()) {
      return List.of();
    }

    return product.getRequiredDocuments().stream()
        .filter(doc -> {
          if (Objects.isNull(doc.getFilter())) {
            return false;
          }
          boolean institutionTypeMatch =
              Objects.nonNull(doc.getFilter().getInstitutionType())
                  && doc.getFilter().getInstitutionType().contains(institutionType);
          boolean originMatch =
              Objects.nonNull(doc.getFilter().getOrigin())
                  && doc.getFilter().getOrigin().contains(origin);
          return institutionTypeMatch && originMatch;
        })
        .toList();
  }


  private void applyParentOnboardingDefaults(Product product, boolean parentIdWasProvided) {
    if (!parentIdWasProvided) {
      return;
    }
    setRequiresParentOnboarding(product, hasParentId(product));
  }

  private boolean hasParentId(Product product) {
    return StringUtils.isNotBlank(product.getParentId());
  }

  private void setRequiresParentOnboarding(Product product, boolean enabled) {
    if (product.getFeatures() == null) {
      if (!enabled) {
        return;
      }
      product.setFeatures(Features.builder().requiresParentOnboarding(true).build());
      return;
    }
    product.getFeatures().setRequiresParentOnboarding(enabled);
  }

  private void validateParentRelationshipSync(Product product) {
    if (!hasParentId(product)) {
      return;
    }
    if (product.getParentId().equals(product.getProductId())) {
      throw new BadRequestException(PARENT_ID_SELF_REFERENCE);
    }
  }

  private Uni<Void> validateParentRelationship(Product product) {
    validateParentRelationshipSync(product);
    if (!hasParentId(product)) {
      return Uni.createFrom().voidItem();
    }

    String parentId = product.getParentId();
    return productRepository
        .findProductById(parentId)
        .onItem()
        .ifNull()
        .failWith(() -> new BadRequestException(String.format(PARENT_PRODUCT_NOT_FOUND, parentId)))
        .onItem()
        .invoke(parent -> validateParentProduct(parent, parentId))
        .replaceWithVoid();
  }

  private void validateParentProduct(Product parent, String parentId) {
    if (hasParentId(parent)) {
      throw new BadRequestException(String.format(PARENT_CANNOT_BE_CHILD, parentId));
    }
  }

  private WorkflowTypeResponse resolveWorkflowType(
      Product product,
      InstitutionType institutionType,
      Origin origin,
      String sanitizedProductId) {

    if (Objects.isNull(product.getWorkflowRules()) || product.getWorkflowRules().isEmpty()) {
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
