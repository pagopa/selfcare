package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.product.entity.ProductStatus;
import lombok.Data;

@Data
public class ProductResource {

    @Schema(description = "${openapi.onboarding.product.model.id}")
    private String id;

    @Schema(description = "${openapi.onboarding.product.model.title}")
    private String title;

    @Schema(description = "${openapi.onboarding.product.model.parentId}")
    private String parentId;

    @Schema(description = "${openapi.onboarding.product.model.status}")
    private ProductStatus status;

    @Schema(description = "${openapi.onboarding.product.model.logo}")
    private String logo;

    @Schema(description = "${openapi.onboarding.product.model.logoBgColor}")
    private String logoBgColor;

}
