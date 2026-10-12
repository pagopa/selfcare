package it.pagopa.selfcare.onboarding.client.model;

import java.util.List;
import lombok.Data;

@Data
public class ProductRoleInfo {

    private boolean skipUserCreation;
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;
    private boolean excludeRoleFromUserGroups;
}
