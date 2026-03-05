package it.pagopa.selfcare.product.entity;

import java.util.List;
import java.util.Objects;

public class ProductRoleInfo {

    private boolean multiroleAllowed;
    private boolean skipUserCreation;

    /**
     * List of phases where addition of the role is allowed
     */
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;

    public boolean isMultiroleAllowed() {
        return multiroleAllowed;
    }

    public void setMultiroleAllowed(boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRoleInfo that)) return false;
        return isMultiroleAllowed() == that.isMultiroleAllowed() && Objects.equals(getPhasesAdditionAllowed(), that.getPhasesAdditionAllowed()) && Objects.equals(getRoles(), that.getRoles()) && Objects.equals(isSkipUserCreation(), that.isSkipUserCreation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isMultiroleAllowed(), getPhasesAdditionAllowed(), getRoles(), isSkipUserCreation());
    }
}
