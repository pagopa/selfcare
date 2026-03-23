package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;

import java.util.List;

public interface ANACService {

    List<Station> loadStations();
}
