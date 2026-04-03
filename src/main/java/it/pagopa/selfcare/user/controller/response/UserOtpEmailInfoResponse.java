package it.pagopa.selfcare.user.controller.response;

import lombok.Data;

@Data
public class UserOtpEmailInfoResponse {

    private String userId;
    private String otpEmail;
    private String otpReferenceInstitutionId;
    private Boolean canUserChangeOtpEmail;

}
