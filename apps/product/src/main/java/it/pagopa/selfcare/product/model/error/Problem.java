package it.pagopa.selfcare.product.model.error;

import java.util.List;
import lombok.Data;

@Data
public class Problem {
  private Integer status;
  private List<ProblemError> errors;
}
