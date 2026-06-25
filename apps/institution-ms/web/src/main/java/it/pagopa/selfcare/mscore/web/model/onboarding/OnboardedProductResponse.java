package it.pagopa.selfcare.mscore.web.model.onboarding;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.web.model.institution.BillingResponse;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
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
    private InstitutionType institutionType;
    private String origin;
    @DecryptIfUuid
    private String originId;
}
