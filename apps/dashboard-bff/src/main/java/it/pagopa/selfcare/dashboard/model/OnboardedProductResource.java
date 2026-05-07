package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.dashboard.model.institution.Billing;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OnboardedProductResource {
    private String productId;
    private String userRole;

    @JsonProperty("productOnBoardingStatus")
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
