package it.pagopa.selfcare.document.model.error;

import lombok.Data;

import java.util.List;

@Data
public class Problem {
  private Integer status;
  private List<ProblemError> errors;
}
