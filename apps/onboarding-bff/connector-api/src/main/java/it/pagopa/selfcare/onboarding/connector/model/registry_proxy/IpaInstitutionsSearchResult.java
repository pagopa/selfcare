package it.pagopa.selfcare.onboarding.connector.model.registry_proxy;

import java.util.List;
import lombok.Data;

@Data
public class IpaInstitutionsSearchResult {

    private List<InstitutionProxyInfo> items;
    private Long count;
}
