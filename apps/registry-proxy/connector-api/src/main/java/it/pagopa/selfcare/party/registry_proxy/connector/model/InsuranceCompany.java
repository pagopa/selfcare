package it.pagopa.selfcare.party.registry_proxy.connector.model;

public interface InsuranceCompany {

    default String getId() {
        return getOriginId();
    }

    String getOriginId();

    String getTaxCode();

    String getDescription();

    String getDigitalAddress();

    String getWorkType();

    String getRegisterType();

    String getAddress();

    default Origin getOrigin() {
        return Origin.IVASS;
    }

    enum Field implements SearchField {
        ID("id"),
        TAX_CODE("taxCode"),
        DESCRIPTION("description"),
        ORIGIN("origin"),
        WORK_TYPE("workType"),
        REGISTER_TYPE("registerType"),
        ORIGIN_ID("originId"),
        ADDRESS("address"),
        DIGITAL_ADDRESS("digitalAddress");

        private final String name;

        Field(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }
}

