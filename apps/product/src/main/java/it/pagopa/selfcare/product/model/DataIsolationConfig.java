package it.pagopa.selfcare.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.pagopa.selfcare.product.model.enums.DatabaseIsolationModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Declares how a product's data must be isolated, so that consuming microservices can route
 * persistence to the right place instead of hardcoding a single database per deployment.
 *
 * <p>The configuration is owned by the product microservice and distributed to consumers through
 * the usual product distribution chain (product-cdc -> blob JSON -> onboarding product SDK).
 *
 * <p>Absent configuration means {@link DatabaseIsolationModel#SHARED}: every product that predates
 * this field keeps its current behaviour, so the field can be rolled out without a data migration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataIsolationConfig {

  /**
   * Database isolation model. Null is read as {@link DatabaseIsolationModel#SHARED} by {@link
   * #resolveDatabaseModel()}.
   */
  private DatabaseIsolationModel database;

  /**
   * Logical name of the dedicated database, meaningful only when {@link #database} is {@code
   * DEDICATED}. It is a logical key, not a connection string or any other credential: consumers map
   * it to a connection they already hold and authenticate with their own managed identity. Secrets
   * MUST NOT be stored here - product configuration is replicated to blob storage and read by every
   * consumer.
   */
  private String databaseName;

  /** Never returns null, so callers do not have to null-check an unconfigured product. */
  public DatabaseIsolationModel resolveDatabaseModel() {
    return database == null ? DatabaseIsolationModel.SHARED : database;
  }

  /**
   * True when this product requires its data to live outside the shared database.
   *
   * <p>Derived helper, deliberately kept out of the JSON contract: it is not a stored field, and
   * exposing it would let clients believe they can set it independently of {@link #database}.
   */
  @JsonIgnore
  @Schema(hidden = true)
  public boolean isDedicatedDatabase() {
    return DatabaseIsolationModel.DEDICATED.equals(resolveDatabaseModel());
  }
}
