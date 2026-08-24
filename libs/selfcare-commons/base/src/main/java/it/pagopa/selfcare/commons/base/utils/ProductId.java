package it.pagopa.selfcare.commons.base.utils;

public enum ProductId {

    PROD_INTEROP("prod-interop"),
    PROD_PN("prod-pn"),
    PROD_FD("prod-fd"),
    PROD_FD_GARANTITO("prod-fd-garantito"),
    PROD_IO("prod-io"),
    PROD_INTEROP_COLL("prod-interop-coll"),
    PROD_IO_SIGN("prod-io-sign"),
    PROD_PAGOPA("prod-pagopa"),
    PROD_IO_PREMIUM("prod-io-premium"),
    PROD_PN_PG("prod-pn-pg");

    private final String value;

    ProductId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
