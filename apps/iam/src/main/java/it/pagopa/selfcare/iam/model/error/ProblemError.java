package it.pagopa.selfcare.iam.model.error;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemError {
  private String code;
  private String detail;
}
