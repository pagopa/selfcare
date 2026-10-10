package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class IpaInstitutionsSearchResponse {
    private List<ProxyInstitutionResponse> items;
    private Long count;
}
