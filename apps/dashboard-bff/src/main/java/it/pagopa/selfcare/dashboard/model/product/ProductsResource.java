package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.product.entity.ProductStatus;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@Data
public class ProductsResource {
    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.products.model.logo}")
    private String logo;

    @Schema(description = "${swagger.dashboard.products.model.logoBgColor}")
    private String logoBgColor;

    @Schema(description = "${swagger.dashboard.products.model.imageUrl}")
    private String imageUrl;

    @Schema(description = "${swagger.dashboard.products.model.title}")
    private String title;

    @Schema(description = "${swagger.dashboard.products.model.description}")
    private String description;

    @ApiModelProperty("${swagger.dashboard.products.model.urlPublic}")
    private String urlPublic;

    @Schema(description = "${swagger.dashboard.products.model.urlBO}")
    private String urlBO;

    @Schema(description = "${swagger.dashboard.products.model.status}")
    private ProductStatus status;

    @Schema(description = "${swagger.dashboard.products.model.delegable}")
    private boolean delegable;

    @Schema(description = "${swagger.dashboard.products.model.children}")
    private List<SubProductResource> children;

    @Schema(description = "${swagger.dashboard.products.model.backOfficeEnvironmentConfigurations}")
    private Collection<BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations;

    @Schema(description = "${swagger.dashboard.products.model.invoiceable}")
    private boolean invoiceable;

}
