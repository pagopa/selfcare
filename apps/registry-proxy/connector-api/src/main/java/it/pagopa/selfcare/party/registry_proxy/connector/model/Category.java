package it.pagopa.selfcare.party.registry_proxy.connector.model;

public interface Category {

    default String getId() {
        return createId(getOrigin(), getCode());
    }

    String getCode();

    String getName();

    String getKind();

    Origin getOrigin();


    enum Field implements SearchField {
        ID("id"),
        CODE("code"),
        NAME("name"),
        KIND("kind"),
        ORIGIN("origin");

        private final String name;

        Field(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    static String createId(Origin origin, String code) {
        return origin + "_" + code;
    }

}
