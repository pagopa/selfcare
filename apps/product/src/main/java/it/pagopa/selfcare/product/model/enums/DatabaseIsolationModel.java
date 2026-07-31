package it.pagopa.selfcare.product.model.enums;

/**
 * How a product's data is physically isolated at database level.
 *
 * <p>See apps/docs/Multitenant/Step_0/EPIC.md sub-task 6: this is the routing signal consumed by
 * every microservice that persists product-scoped data, so that a single backend deployment can
 * serve products with different isolation requirements.
 */
public enum DatabaseIsolationModel {

  /**
   * Data lives in the platform's common database, alongside every other SHARED product. Isolation
   * is logical only (discriminator fields on each document). This is the default and the model
   * every product uses today.
   */
  SHARED,

  /**
   * Data lives in a database dedicated to this product. Consumers MUST route their connections
   * accordingly and MUST fail closed when the target database cannot be resolved, rather than
   * silently falling back to the shared one.
   */
  DEDICATED
}
