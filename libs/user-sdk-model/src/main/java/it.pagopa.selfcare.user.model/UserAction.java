package it.pagopa.selfcare.user.model;

import lombok.Getter;

@Getter
public enum UserAction {

    UPLOAD_LOGO("Selc", "UploadLogo"),
    VIEW_BILLING("Selc","ViewBilling"),
    REQUEST_PRODUCT_ACCESS("Selc","RequestProductAccess"),
    LIST_AVAILABLE_PRODUCTS("Selc","ListAvailableProducts"),
    LIST_ACTIVE_PRODUCTS("Selc","ListActiveProducts"),
    ACCESS_PRODUCT_BACKOFFICE("Selc","AccessProductBackoffice"),
    VIEW_MANAGED_INSTITUTIONS("Selc","ViewManagedInstitutions"),
    VIEW_DELEGATION("Selc","ViewDelegations"),
    MANAGE_PRODUCT_USER("Selc","ManageProductUsers"),
    MANAGE_PRODUCT_GROUPS("Selc","ManageProductGroups"),
    CREATE_DELEGATION("Selc","CreateDelegation"),
    VIEW_INSTITUTION_DATA("Selc","ViewInstitutionData"),
    UPDATE_INSTITUTION_DATA("Selc","UpdateInstitutionData");

    private final String functionality;
    private final String namespace;

    UserAction(String namespace, String functionality) {
        this.functionality = functionality;
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", namespace, functionality);
    }
}
