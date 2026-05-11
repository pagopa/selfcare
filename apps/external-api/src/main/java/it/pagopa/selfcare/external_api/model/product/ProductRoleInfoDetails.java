package it.pagopa.selfcare.external_api.model.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
public class ProductRoleInfoDetails {

    @Schema(description = "${swagger.external_api.product-role-info.model.multiroleAllowed}")
    @JsonProperty(required = true)
    @NotNull
    private Boolean multiroleAllowed;

    @Schema(description = "${swagger.external_api.product-role-info.model.roles}")
    @JsonProperty(required = true)
    @NotEmpty
    @Valid
    private List<ProductRole> roles;

    public ProductRoleInfoDetails(ProductRoleInfo productRoleInfo) {
        multiroleAllowed = productRoleInfo.isMultiroleAllowed();
        setRoles(productRoleInfo.getRoles());
    }

    public void setRoles(List<ProductRoleInfo.ProductRole> roles) {
        if (roles != null) {
            this.roles = roles.stream()
                    .map(ProductRole::new)
                    .collect(Collectors.toList());
        }
    }
}
