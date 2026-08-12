package it.pagopa.selfcare.product.entity;

/**
 * How a product's data is physically isolated at database level.
 *
 * <p>Mirrors the enum owned by the product microservice and distributed to consumers through the
 * product JSON. See apps/docs/Multitenant/Step_0/EPIC.md sub-task 6.
 */
public enum DatabaseIsolationModel {

    /** Data lives in the platform's common database; isolation is logical only. Default. */
    SHARED,

    /**
     * Data lives in a database dedicated to this product. Consumers MUST route their connections
     * accordingly and MUST fail closed when the target database cannot be resolved.
     */
    DEDICATED
}
