package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.product.entity.ProductStatus;
import lombok.Data;

@Data
public class ProductResource {

    @ApiModelProperty(value = "${swagger.onboarding.product.model.id}")
    private String id;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.title}")
    private String title;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.parentId}")
    private String parentId;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.status}")
    private ProductStatus status;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.logo}")
    private String logo;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.logoBgColor}")
    private String logoBgColor;

}
