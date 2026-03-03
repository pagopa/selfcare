package it.pagopa.selfcare.onboarding.exception.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemError {
    private String code;
    private String detail;
}
