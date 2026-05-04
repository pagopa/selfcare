package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.Data;

@Data
public class RelationshipPageElement {
    private String userId;
    private OnboardedProduct product;
}
