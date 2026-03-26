package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InstitutionOnboarding {
    private String productId;
    private String tokenId;
    private String status;
    private String contract;
    private String pricingPlan;
    private Billing billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
}
