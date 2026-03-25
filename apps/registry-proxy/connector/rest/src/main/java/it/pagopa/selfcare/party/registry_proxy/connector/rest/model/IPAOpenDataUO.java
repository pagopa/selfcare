package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;

public class IPAOpenDataUO extends IPAOpenDataUOTemplate {

    @Override
    public Origin getOrigin() {
        return Origin.IPA;
    }

}
