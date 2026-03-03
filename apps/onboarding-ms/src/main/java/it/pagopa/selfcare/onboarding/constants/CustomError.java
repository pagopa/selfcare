package it.pagopa.selfcare.onboarding.constants;

public enum CustomError {

    DEFAULT_ERROR("0000", ""),

    ROLES_NOT_ADMITTED_ERROR("0034","Roles %s are not admitted for this operation"),
    AOO_NOT_FOUND("0000","AOO  %s not found"),
    STATION_NOT_FOUND("0000","Station %s not found"),
    INSURANCE_NOT_FOUND("0000","Insurance %s not found"),
    UO_NOT_FOUND("0000","UO %s not found"),
    ONBOARDING_INFO_FILTERS_ERROR("0052", "Invalid filters parameters to retrieve onboarding info"),
    INSTITUTION_NOT_ONBOARDED_BY_FILTERS("0004", "Has not been found an onboarded Institution with the provided filters"),

    INSTITUTION_NOT_FOUND("0000","Institution with taxCode %s origin %s originId %s subunitCode %s not found"),
    USERS_UPDATE_NOT_ALLOWED("0025", "Invalid users information provided"),

    DENIED_NO_BILLING("0040","Recipient code linked to an institution with invoicing service not active"),
    DENIED_NO_ASSOCIATION("0041","Recipient code not linked to any institution"),
    TOO_MANY_CONTRACTS("0043","Too many contracts provided"),
    INDIVIDUAL_ONBOARDING_NOT_ALLOWED("0053","Individual onboarding is not allowed for this product"),
    COMPANY_ONBOARDING_NOT_ALLOWED("0054","Company onboarding is not allowed for this product"),

    TOKEN_NOT_FOUND_OR_ALREADY_DELETED("Token with id %s not found or already deleted"),
    AT_LEAST_ONE_PRODUCT_ROLE_REQUIRED("At least one Product role related to %s Party role is required"),
    MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE("More than one Product role related to %s Party role is available. Cannot automatically set the Product role"),
    ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE("Institution with external id '%s' is not allowed to onboard '%s' product"),
    ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED("Onboarding with id %s not found or already deleted"),
    UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED("Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed."),
    NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY("User is not manager of the institution on the registry"),
    ERROR_IPA("Codice Fiscale non presente su IPA"),
    ERROR_TAXCODE("Il Codice Fiscale è obbligatorio"),
    ERROR_SUBUNIT_TYPE("SubunitType non valido"),
    ERROR_AOO_UO("In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO"),
    ERROR_VATNUMBER("La Partita IVA è obbligatoria"),
    ERROR_ADMIN_NAME("Nome Amministratore Ente Aggregato è obbligatorio"),
    ERROR_ADMIN_SURNAME("Cognome Amministratore Ente Aggregato è obbligatorio"),
    ERROR_ADMIN_EMAIL("Email Amministratore Ente Aggregato è obbligatorio"),
    ERROR_ADMIN_TAXCODE("Codice Fiscale Amministratore Ente Aggregato è obbligatorio"),
    ERROR_IBAN("IBAN è obbligatorio"),
    ERROR_CODICE_SDI("Codice SDI è obbligatorio"),
    ERROR_ADMIN_NAME_MISMATCH("Nome non corretto o diverso dal Codice Fiscale"),
    ERROR_ADMIN_SURNAME_MISMATCH("Cognome non corretto o diverso dal Codice Fiscale"),
    ERROR_TAXCODE_LENGTH("Il Codice Fiscale non è valido"),
    ERROR_VATNUMBER_LENGTH("La Partita IVA non è valida"),
    ;


    private final String code;
    private final String detail;

    CustomError(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    CustomError(String detail) {
        this("0000", detail);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return detail;
    }
}
