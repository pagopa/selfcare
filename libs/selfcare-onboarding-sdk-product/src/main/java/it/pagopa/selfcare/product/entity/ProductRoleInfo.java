package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ProductRoleInfo {

    private boolean skipUserCreation;
    /**
     * List of phases where addition of the role is allowed
     */
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;

    public boolean isSkipUserCreation() {
        return skipUserCreation;
    }

    public void setSkipUserCreation(boolean skipUserCreation) {
        this.skipUserCreation = skipUserCreation;
    }

    public List<String> getPhasesAdditionAllowed() {
        return phasesAdditionAllowed;
    }

    public void setPhasesAdditionAllowed(List<String> phasesAdditionAllowed) {
        this.phasesAdditionAllowed = phasesAdditionAllowed;
    }

    public List<ProductRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ProductRole> roles) {
        this.roles = roles;
    }

}
