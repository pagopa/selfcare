package it.pagopa.selfcare.document.model.error;

import java.util.List;
import lombok.Data;

@Data
public class Problem {
  private Integer status;
  private List<ProblemError> errors;
}
