package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.client.model.AggregateResult;
import lombok.Data;

import java.util.List;

@Data
public class VerifyAggregatesResponse {
    private List<AggregateResult> aggregates ;
    private List<RowErrorResponse> errors;
}
