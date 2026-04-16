package it.pagopa.selfcare.party.registry_proxy.connector.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AdEResultCodeEnum {

        CODE_00("00"),

        CODE_01("01"),

        CODE_02("02"),

        CODE_03("03"),

        UNKNOWN("UNKNOWN");

        private final String value;

        AdEResultCodeEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
}
