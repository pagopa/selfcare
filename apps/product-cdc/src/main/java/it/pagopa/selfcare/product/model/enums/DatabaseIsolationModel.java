package it.pagopa.selfcare.product.model.enums;

/**
 * How a product's data is physically isolated at database level. Mirrors the enum owned by the
 * product microservice; kept here because product-cdc duplicates the product model it reads from
 * Mongo before mapping it to the SDK entity published to blob storage.
 */
public enum DatabaseIsolationModel {
  SHARED,
  DEDICATED
}
