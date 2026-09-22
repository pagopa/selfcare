package it.pagopa.selfcare.onboarding.connector.rest.model;

import java.util.List;
import lombok.Data;

@Data
public class IpaInstitutionsSearchResponse {

    private List<ProxyInstitutionResponse> items;
    private Long count;
}
