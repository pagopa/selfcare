package it.pagopa.selfcare.onboarding.exception.model;

import lombok.Data;

import java.util.List;

@Data
public class Problem {
    private Integer status;
    private List<ProblemError> errors;
}
