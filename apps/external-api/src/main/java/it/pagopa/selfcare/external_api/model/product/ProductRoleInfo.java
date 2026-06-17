package it.pagopa.selfcare.external_api.model.product;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ProductRoleInfo {

    private boolean multiroleAllowed;
    private boolean skipUserCreation;
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;


    @Data
    @EqualsAndHashCode(of = "code")
    public static class ProductRole {
        private String code;
        private String label;
        private String productLabel;
        private String description;
        private List<String> multiroleGroups;

    }



}
