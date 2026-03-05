package it.pagopa.selfcare.onboarding.common;

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
    PROD_DASHBOARD_PSP("prod-dashboard-psp"),
    PROD_REGISTRO_BENI("prod-registro-beni"),
    PROD_IDPAY_MERCHANT("prod-idpay-merchant"),
    PROD_IDPAY_GI("prod-idpay-gi");

    private final String value;

    ProductId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
