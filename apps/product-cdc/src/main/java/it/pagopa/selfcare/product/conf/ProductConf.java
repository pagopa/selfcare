package it.pagopa.selfcare.product.conf;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
@Data
public class ProductConf {
  @ConfigProperty(name = "product-cdc.blob-storage.container-product")
  String containerProduct;

  @ConfigProperty(name = "product-cdc.blob-storage.filepath-product")
  String filepathProduct;

  @ConfigProperty(name = "product-cdc.blob-storage.connection-string-product")
  String connectionStringProduct;

  void onStart(@Observes StartupEvent ev) {
    log.info(String.format("Database %s is starting...", Product.mongoDatabase().getName()));
  }

  @ApplicationScoped
  public ProductService productService(){
    return new ProductServiceCacheable(connectionStringProduct, containerProduct, filepathProduct);
  }

  @ApplicationScoped
  public AzureBlobClient azureBobClientContract(){
    return new AzureBlobClientDefault(connectionStringProduct, containerProduct);
  }
}
