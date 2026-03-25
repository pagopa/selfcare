package it.pagopa.selfcare.party.registry_proxy.connector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Entity {
    ENTITY_TYPE("ENTITY_TYPE"),
    CATEGORY("CATEGORY"),
    INSTITUTION("INSTITUTION"),
    AOO("AOO"),
    UO("UO"),
    INSURANCE_COMPANY("INSURANCE_COMPANY"),
    STATION("STATION");

    private final String value;

    Entity(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    @JsonCreator
    public static Entity fromValue(String value) {
        return Arrays.stream(values())
                .filter(origin -> origin.toString().equals(value))
                .findAny()
                .orElseThrow();
    }

}
