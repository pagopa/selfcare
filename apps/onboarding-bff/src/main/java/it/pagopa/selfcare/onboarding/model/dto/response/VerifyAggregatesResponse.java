package it.pagopa.selfcare.onboarding.model.dto.response;

import it.pagopa.selfcare.onboarding.client.model.AggregateResult;
import lombok.Data;

import java.util.List;

@Data
public class VerifyAggregatesResponse {
    private List<AggregateResult> aggregates ;
    private List<RowErrorResponse> errors;
}
