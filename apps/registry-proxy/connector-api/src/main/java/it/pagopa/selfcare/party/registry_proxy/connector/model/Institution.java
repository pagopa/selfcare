package it.pagopa.selfcare.party.registry_proxy.connector.model;

public interface Institution {

    String getId();

    String getOriginId();

    String getO();

    String getOu();

    String getAoo();

    String getTaxCode();

    String getCategory();

    String getDescription();

    String getDigitalAddress();

    String getAddress();

    String getZipCode();

    Origin getOrigin();
    String getIstatCode();


    enum Field implements SearchField {
        ID("id"),
        ORIGIN_ID("originId"),
        O("o"),
        OU("ou"),
        AOO("aoo"),
        TAX_CODE("taxCode"),
        CATEGORY("category"),
        DESCRIPTION("description"),
        DIGITAL_ADDRESS("digitalAddress"),
        ADDRESS("address"),
        ZIP_CODE("zipCode"),
        ORIGIN("origin"),

        DESCRIPTIONFULL("descriptionfull"),
        ISTAT_CODE("istatCode");

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
