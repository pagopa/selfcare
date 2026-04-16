package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@FieldNameConstants(asEnum = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserBinding {
    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    private List<OnboardedProduct> products;

    public UserBinding(String institutionId, List<OnboardedProduct> products) {
        this.institutionId = institutionId;
        this.products = products;
    }
}
