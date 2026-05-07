package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProductInfoResource {
    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.products.model.title}")
    private String title;

    @Schema(description = "${swagger.dashboard.products.model.roleInfos}")
    private List<ProductRoleInfoResource> roleInfos;
}
