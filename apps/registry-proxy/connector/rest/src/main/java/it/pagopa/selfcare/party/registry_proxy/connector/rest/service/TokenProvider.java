package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;

public interface TokenProvider {
    ClientCredentialsResponse getTokenPdnd(PdndSecretValue pdndSecretValue);
}
