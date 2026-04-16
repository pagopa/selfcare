package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductsManagement {
    private String product;
    private String pricingPlan;
    private BillingResponse billing;
}
