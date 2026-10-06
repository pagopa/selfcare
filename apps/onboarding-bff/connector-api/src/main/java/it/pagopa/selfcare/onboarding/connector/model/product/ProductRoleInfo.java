package it.pagopa.selfcare.onboarding.connector.model.product;

import lombok.Data;

import java.util.List;

@Data
public class ProductRoleInfo {

    private boolean skipUserCreation;
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;
    private boolean excludeRoleFromUserGroups;
}

