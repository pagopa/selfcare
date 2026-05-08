package it.pagopa.selfcare.dashboard.model.institution;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OnboardedProduct {
    private String productId;
    private String userRole;
    private RelationshipState status;
    private boolean authorized;
    private Billing billing;
    private List<String> userProductActions;
    private Boolean isAggregator;
    private String origin;
    private String originId;
    private InstitutionType institutionType;
    private OffsetDateTime createdAt;
    private String tokenId;
}
