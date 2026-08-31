package it.pagopa.selfcare.auth.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.auth.model.OtpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpForbidden {

  private String detail;
  private String instance;
  private Integer status;
  private String title;
  private String type;
  private OtpForbiddenCode otpForbiddenCode;
  private Integer remainingAttempts;
  private OtpStatus otpStatus;
}
