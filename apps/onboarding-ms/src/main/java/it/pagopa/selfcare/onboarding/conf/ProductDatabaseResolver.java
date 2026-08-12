package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.arc.Unremovable;
import it.pagopa.selfcare.onboarding.exception.UnresolvableProductDatabaseException;
import it.pagopa.selfcare.product.entity.DatabaseIsolationModel;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves which MongoDB database holds a given product's data, from the {@code dataIsolation}
 * block the product microservice publishes (EPIC sub-task 6).
 *
 * <p>Products declared {@code SHARED}, and products that declare nothing at all, resolve to the
 * database this service has always used. Products declared {@code DEDICATED} resolve to their own
 * database, in the same Cosmos DB account and reached with the same managed identity, so no
 * additional credential is introduced.
 *
 * <p>Resolution is deliberately <b>fail-closed</b>: a product that declares {@code DEDICATED} but
 * whose database cannot be determined raises {@link UnresolvableProductDatabaseException} instead of
 * falling back to the shared database. Falling back would silently write a product's data into a
 * database it was explicitly configured out of, which is the isolation breach this configuration
 * exists to prevent.
 */
@ApplicationScoped
@Unremovable
public class ProductDatabaseResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductDatabaseResolver.class);

  private final ProductService productService;
  private final String sharedDatabase;

  @Inject
  public ProductDatabaseResolver(
      ProductService productService,
      @ConfigProperty(name = "quarkus.mongodb.database") String sharedDatabase) {
    this.productService = productService;
    this.sharedDatabase = sharedDatabase;
  }

  /** The database every SHARED product lives in. */
  public String sharedDatabase() {
    return sharedDatabase;
  }

  /**
   * @param productId product owning the data being accessed; blank means "no product context" and
   *     resolves to the shared database
   * @return the database name to route to, never null
   * @throws UnresolvableProductDatabaseException when a product requires a dedicated database that
   *     cannot be resolved
   */
  public String resolveDatabase(String productId) {
    if (StringUtils.isBlank(productId)) {
      return sharedDatabase;
    }

    Optional<Product> product = findProduct(productId);
    if (product.isEmpty()) {
      // An unknown product cannot be proven to be SHARED, so it is not assumed to be.
      throw new UnresolvableProductDatabaseException(
          String.format("Cannot resolve database: product %s is unknown", productId));
    }

    Product resolved = product.get();
    if (!DatabaseIsolationModel.DEDICATED.equals(resolved.resolveDatabaseIsolationModel())) {
      return sharedDatabase;
    }

    String databaseName =
        resolved.getDataIsolation() == null ? null : resolved.getDataIsolation().getDatabaseName();
    if (StringUtils.isBlank(databaseName)) {
      throw new UnresolvableProductDatabaseException(
          String.format("Product %s declares a DEDICATED database but no databaseName", productId));
    }
    return databaseName;
  }

  private Optional<Product> findProduct(String productId) {
    try {
      return Optional.ofNullable(productService.getProductRaw(productId));
    } catch (RuntimeException e) {
      LOGGER.warn("Unable to load product {} while resolving its database", productId, e);
      return Optional.empty();
    }
  }
}
