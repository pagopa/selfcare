package it.pagopa.selfcare.auth.controller.response;

public enum OtpForbiddenCode {
    CODE_001("Wrong OTP Code"),
    CODE_002("Max attempts reached");

    OtpForbiddenCode(String desc) {

    }
}
