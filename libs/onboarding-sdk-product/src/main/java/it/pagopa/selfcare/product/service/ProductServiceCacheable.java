package it.pagopa.selfcare.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductServiceCacheable implements ProductService {
  protected LocalDateTime productLastModifiedDate;
  private final AzureBlobClient azureBlobClient;
  private ProductServiceDefault productService;
  final String filePath;

  public ProductServiceCacheable(String connectionString, String containerName, String filePath) {
    this.azureBlobClient = new AzureBlobClientDefault(connectionString, containerName);
    this.filePath = filePath;
    refreshProduct();
  }

  public ProductServiceCacheable(AzureBlobClient azureBlobClient, String filePath) {
    this.azureBlobClient = azureBlobClient;
    this.filePath = filePath;
    refreshProduct();
  }

  public void refreshProduct() {
    LocalDateTime currentLastModifiedDate =
        azureBlobClient.getProperties(filePath).getLastModified().toLocalDateTime();
    if (productLastModifiedDate == null
        || currentLastModifiedDate.isAfter(productLastModifiedDate)) {
      String productJsonString = azureBlobClient.getFileAsText(filePath);
      try {
        this.productService = new ProductServiceDefault(productJsonString);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      this.productLastModifiedDate = currentLastModifiedDate;
    }
  }

  @Override
  public List<Product> getProducts(boolean rootOnly, boolean valid) {
    refreshProduct();
    return productService.getProducts(rootOnly, valid);
  }

  @Override
  public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {
    refreshProduct();
    productService.validateRoleMappings(roleMappings);
  }

  @Override
  public Product getProduct(String productId) {
    refreshProduct();
    return productService.getProduct(productId);
  }

  /**
   * Return a product present on the map by productId without any filter retrieving data from
   * institutionContractMappings map
   *
   * @param productId String
   * @return Product
   * @throws IllegalArgumentException if @param id is null
   * @throws ProductNotFoundException if product is not found
   */
  @Override
  public Product getProductRaw(String productId) {
    return productService.getProduct(productId);
  }

  @Override
  public Product getProductIsValid(String productId) {
    refreshProduct();
    return productService.getProductIsValid(productId);
  }

  @Override
  public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
    refreshProduct();
    return productService.validateProductRole(productId, productRole, role);
  }

  @Override
  public boolean verifyAllowedByInstitutionTaxCode(String productId, String taxCode) {
    refreshProduct();
    return productService.verifyAllowedByInstitutionTaxCode(productId, taxCode);
  }

  @Override
  public Integer getProductExpirationDate(String productId) {
    refreshProduct();
    return productService.getProductExpirationDate(productId);
  }

}
