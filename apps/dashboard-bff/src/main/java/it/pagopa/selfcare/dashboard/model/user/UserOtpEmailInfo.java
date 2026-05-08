package it.pagopa.selfcare.dashboard.model.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserOtpEmailInfo {

    private String userId;
    private String otpEmail;
    private String otpReferenceInstitutionId;
    private Boolean canUserChangeOtpEmail;

}
