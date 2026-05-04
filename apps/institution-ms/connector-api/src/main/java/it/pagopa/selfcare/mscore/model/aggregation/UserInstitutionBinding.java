package it.pagopa.selfcare.mscore.model.aggregation;

import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserInstitutionBinding {
    private String institutionId;
    private OnboardedProduct products;
}
