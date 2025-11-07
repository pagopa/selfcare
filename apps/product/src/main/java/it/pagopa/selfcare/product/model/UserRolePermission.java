package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRolePermission {
    private boolean multiroleAllowed;
    private boolean skipUserCreation;
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;
}
