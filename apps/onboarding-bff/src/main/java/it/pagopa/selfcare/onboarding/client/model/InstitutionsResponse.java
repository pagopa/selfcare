package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionsResponse {

    private List<InstitutionResponse> institutions;
}
