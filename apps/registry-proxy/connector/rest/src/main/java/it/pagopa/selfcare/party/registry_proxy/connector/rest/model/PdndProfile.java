package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

/**
 * Identifies the PDND subscription (set of credentials) used to authenticate a request.
 * It is part of the cache key so that results fetched with one subscription are not
 * served to callers that must use the other subscription.
 */
public enum PdndProfile {
    SELFCARE,
    INVITALIA
}

