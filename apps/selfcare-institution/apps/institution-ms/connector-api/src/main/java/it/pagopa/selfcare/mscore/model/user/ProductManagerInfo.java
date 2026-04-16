package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductManagerInfo {

    private String userId;
    private Institution institution;
    private List<OnboardedProduct> products;
}
