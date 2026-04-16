package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

public enum InipecScopeEnum {

    PEC("pec-pa"),
    SEDE("sede-impresa-pa"),
    LEGALE_RAPPRESENTANTE("lr-pa");

    private final String value;

    InipecScopeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
