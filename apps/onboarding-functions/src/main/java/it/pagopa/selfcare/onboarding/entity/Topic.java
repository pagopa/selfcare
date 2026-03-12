package it.pagopa.selfcare.onboarding.entity;

public enum Topic {
    SC_CONTRACTS_FD("Selfcare-FD"),
    SC_CONTRACTS_SAP("SC-Contracts-SAP"),
    SC_CONTRACTS("SC-Contracts");

    Topic(String value) {
        this.value = value;
    }
    private final String value;

    public String getValue() {
        return value;
    }
}