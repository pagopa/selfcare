package it.pagopa.selfcare.user.constant;

public enum CustomError {
    USER_NOT_FOUND_ERROR("0031", "User having userId %s not found"),
    USER_INSTITUTION_NOT_FOUND_ERROR("0031", "User having userId [%s] and institutionId [%s] not found"),
    STATUS_IS_MANDATORY("0000", "STATUS IS MANDATORY"),
    USER_TO_UPDATE_NOT_FOUND("0000", "USER TO UPDATE NOT FOUND"),
    USERS_TO_UPDATE_NOT_FOUND("0000", "USERS TO UPDATE NOT FOUND");

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
