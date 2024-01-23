package it.pagopa.selfcare.user.constant;

public enum CustomError {
    ROLE_NOT_FOUND("0000", "ROLE_NOT_FOUND"),
    ROLE_IS_NULL("0000", "ROLE_IS_NULL - Role is required if productRole is present"),
    USER_NOT_FOUND_ERROR("0031", "User having userId %s not found"),

    PRODUCT_ROLE_NOT_FOUND("0000", "PRODUCT_ROLE_NOT_FOUND");

    private final String code;
    private final String detail;

    CustomError(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return detail;
    }
}
