package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution;


import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardedProductResponse {
    private String productId;
    private String tokenId;
    private RelationshipState status;
    private BillingResponse billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isAggregator;
    private String institutionType;
    private String origin;
    private String originId;
}
