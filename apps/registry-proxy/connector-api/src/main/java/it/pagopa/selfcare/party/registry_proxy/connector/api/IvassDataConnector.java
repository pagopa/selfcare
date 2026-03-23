package it.pagopa.selfcare.party.registry_proxy.connector.api;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;

import java.util.List;

public interface IvassDataConnector {
    List<InsuranceCompany> getInsurances();
}
