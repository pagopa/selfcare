package it.pagopa.selfcare.dashboard.model.product;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ProductRoleInfo {

    private boolean multiroleAllowed;
    private List<ProductRole> roles;


    @Data
    @EqualsAndHashCode(of = "code")
    public static class ProductRole {
        private String code;
        private String label;
        private String description;
    }

}
