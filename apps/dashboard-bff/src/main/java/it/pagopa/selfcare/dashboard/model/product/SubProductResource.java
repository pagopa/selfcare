package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.product.entity.ProductStatus;
import lombok.Data;

@Data
public class SubProductResource {

    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.products.model.title}")
    private String title;

    @Schema(description = "${swagger.dashboard.products.model.productOnBoardingStatus}")
    private ProductOnBoardingStatus productOnBoardingStatus;

    @Schema(description = "${swagger.dashboard.products.model.status}")
    private ProductStatus status;

    @Schema(description = "${swagger.dashboard.products.model.delegable}")
    private boolean delegable;

    @Schema(description = "${swagger.dashboard.products.model.imageUrl}")
    private String imageUrl;

    @Schema(description = "${swagger.dashboard.products.model.logo}")
    private String logo;

    @Schema(description = "${swagger.dashboard.products.model.logoBgColor}")
    private String logoBgColor;

    @Schema(description = "${swagger.dashboard.products.model.description}")
    private String description;

    @Schema(description = "${swagger.dashboard.products.model.urlPublic}")
    private String urlPublic;

    @Schema(description = "${swagger.dashboard.products.model.invoiceable}")
    private boolean invoiceable;

}
