package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class OnboardedProductResource {
    @Schema(description = "${swagger.external_api.products.model.id}")
    private String productId;
    @Schema(description = "${swagger.external_api.user.model.role}")
    private PartyRole role;
    @Schema(description = "${swagger.external_api.user.model.productRoles}")
    private List<String> roles;
    @Schema(description = "${swagger.external_api.onboarding.model.createdAt}")
    private OffsetDateTime createdAt;
}
