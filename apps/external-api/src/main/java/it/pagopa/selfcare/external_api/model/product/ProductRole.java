package it.pagopa.selfcare.external_api.model.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(of = "code")
@NoArgsConstructor
public class ProductRole {

    @Schema(description = "${swagger.external_api.product-role.model.code}")
    @JsonProperty(required = true)
    @NotBlank
    private String code;

    @Schema(description = "${swagger.external_api.product-role.model.label}")
    @JsonProperty(required = true)
    @NotBlank
    private String label;

    @Schema(description = "${swagger.external_api.product-role.model.description}")
    @JsonProperty(required = true)
    @NotBlank
    private String description;


    public ProductRole(ProductRoleInfo.ProductRole productRole) {
        code = productRole.getCode();
        label = productRole.getLabel();
        description = productRole.getDescription();
    }

}