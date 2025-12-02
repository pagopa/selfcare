package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMapping {
    private String role;
    private boolean multiroleAllowed;
    private List<String> phasesAdditionAllowed;
    private boolean skipUserCreation;
    private List<BackOfficeRole> backOfficeRoles;
}