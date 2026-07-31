package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.DatabaseIsolationModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Declares how a product's data must be isolated. Read from Mongo and forwarded, unchanged, to the
 * product JSON consumers use to route their persistence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataIsolationConfig {

  private DatabaseIsolationModel database;

  /** Logical database name, meaningful only when {@link #database} is DEDICATED. Never a secret. */
  private String databaseName;
}
