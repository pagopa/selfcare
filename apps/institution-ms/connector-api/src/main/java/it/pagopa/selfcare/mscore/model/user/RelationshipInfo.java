package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class RelationshipInfo {

    Institution institution;
    String userId;
    OnboardedProduct onboardedProduct;
}
