package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class IpaInstitutionsSearchResult {
    private List<InstitutionProxyInfo> items;
    private Long count;
}
