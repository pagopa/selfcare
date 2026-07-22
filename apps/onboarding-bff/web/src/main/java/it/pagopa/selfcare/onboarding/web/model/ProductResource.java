package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.product.entity.ProductStatus;
import lombok.Data;

@Data
public class ProductResource {

    @Schema(description = "${swagger.onboarding.product.model.id}")
    private String id;

    @Schema(description = "${swagger.onboarding.product.model.title}")
    private String title;

    @Schema(description = "${swagger.onboarding.product.model.parentId}")
    private String parentId;

    @Schema(description = "${swagger.onboarding.product.model.status}")
    private ProductStatus status;

    @Schema(description = "${swagger.onboarding.product.model.logo}")
    private String logo;

    @Schema(description = "${swagger.onboarding.product.model.logoBgColor}")
    private String logoBgColor;

}
