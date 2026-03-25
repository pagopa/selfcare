package it.pagopa.selfcare.party.registry_proxy.connector.api;

import java.io.ByteArrayInputStream;
import java.util.Optional;

public interface AnacDataConnector {
    Optional<ByteArrayInputStream> getANACData();
}
