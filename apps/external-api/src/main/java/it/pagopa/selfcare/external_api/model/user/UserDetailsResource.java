package it.pagopa.selfcare.external_api.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedProductResource;
import lombok.Data;

@Data
public class UserDetailsResource {
    @Schema(description = "${swagger.external_api.user.model.id}")
    private String id;
    @Schema(description = " swagger.external_api.institutions.model.id")
    private String institutionId;
    @Schema(description = "${swagger.external_api.product.onboardedProduct}")
    private OnboardedProductResource onboardedProductDetails;
}
