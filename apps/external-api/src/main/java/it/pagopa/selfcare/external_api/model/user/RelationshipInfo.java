package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.external_api.model.institution.Institution;
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
