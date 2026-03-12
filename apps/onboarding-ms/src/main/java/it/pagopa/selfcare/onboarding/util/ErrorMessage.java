package it.pagopa.selfcare.onboarding.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorMessage {

    GET_USER_INSTITUTION_RELATIONSHIP_ERROR("0023", "Error while retrieving user relationships"),
    GET_INSTITUTION_BY_ID_ERROR("0040", "Error while retrieving institution having id %s"),
    GET_INSTITUTION_BY_EXTERNAL_ID_ERROR("0041", MessageValue.ERROR_WHILE_RETRIEVING_INSTITUTION_HAVING_EXTERNAL_ID),
    INSTITUTION_MANAGER_ERROR("0042", MessageValue.ERROR_WHILE_RETRIEVING_INSTITUTION_HAVING_EXTERNAL_ID),
    INSTITUTION_BILLING_ERROR("0044", MessageValue.ERROR_WHILE_RETRIEVING_INSTITUTION_HAVING_EXTERNAL_ID),
    CREATE_INSTITUTION_ERROR("0037", "Error while creating requested institution"),
    INSTITUTION_INFOCAMERE_NOTFOUND("0039", "Institution %s not found on INFOCAMERE"),
    ONBOARDING_OPERATION_ERROR("0017", "Error while performing onboarding operation"),
    CREATE_DELEGATION_ERROR("0027", "Error while creating requested delegation"),
    ONBOARDING_VERIFICATION_ERROR("0015", "Error while verifying onboarding"),
    GETTING_ONBOARDING_INFO_ERROR("0016", "Error while getting onboarding info"),
    GET_PRODUCTS_ERROR("0031", "Error while getting products"),
    PRODUCT_ALREADY_ONBOARDED("0032", "Product %s already onboarded for institution having externalId %s"),
    PRODUCT_NOT_ONBOARDED("0033", "Product %s not onboarded or isn't ACTIVE for institution having externalId %s"),
    INVALID_REFERENCE_ONBORADING("0034", "Reference onboarding id is not present or reference onboarding doesn't exist"),
    PARENT_PRODUCT_NOT_ONBOARDED("0035", "Parent product %s not onboarded for institution having externalId %s"),
    CONTRACT_PATH_ERROR("0100", "Contract Path is required"),
    MANAGER_EMAIL_NOT_FOUND("0101", "Manager email not found"),
    VERIFY_TOKEN_FAILED("0041", "Something went wrong trying to verify token"),

    SIGNATURE_NOT_FOUND("002-1007", "No signature found"),
    SIGNATURE_VALIDATION_ERROR("002-1004", "The tax code related to signature does not match anyone contained in the relationships"),
    ORIGINAL_DOCUMENT_NOT_FOUND("002-1008", "Original document information not found"),

    INSTITUTION_NOT_ONBOARDED("002-1009", "Institution having externalId %s has already onboarded for product %s"),
    DOCUMENT_VALIDATION_FAIL("002-1000", "Error trying to validate document, due: %s"),
    INVALID_SIGNATURE_FORMS("002-1003", "Only CAdES signature form is admitted. Invalid signatures forms detected: %s"),
    INVALIDATE_ONBOARDING_ERROR("0022", "Error while invalidating onboarding"),
    CONFIRM_ONBOARDING_ERROR("0021", "Error while confirming onboarding"),
    INVALID_DOCUMENT_SIGNATURE("002-1002", "Document signature is invalid"),
    INVALID_CONTRACT_DIGEST("002-1001", "Invalid file digest"),

    INVALIDE_SIGNATURE_TAX_CODE_FORMAT("002-1005", "Invalid tax code format found in digital signature"),
    TAX_CODE_NOT_FOUND_IN_SIGNATURE("002-1006", "No tax code has been found in digital signature"),
    INVALID_SIGNATURE_TAX_CODE("002-1004", "The tax code related to signature does not match anyone contained in the relationships"),
    UNABLE_TO_DOWNLOAD_FILE("1102", "Unable to download template %s"),
    INVALID_SIGNATURE("002-1005", "Signature not valid: "),
    ERROR_DURING_SEND_MAIL("0000", "Error during send mail"),
    ERROR_DURING_UPLOAD_FILE("0000", "Error during upload file %s"),

    ERROR_DURING_DELETED_FILE("0000", "Error during deleted file %s"),
    ERROR_DURING_DOWNLOAD_FILE("0000", "Error during download file %s"),
    ERROR_DURING_COMPRESS_FILE("0000", "Error compressing the file %s"),
    RETRIEVING_USER_RELATIONSHIP_ERROR("0023", "Error while retrieving user relationships"),
    ACTIVATE_RELATIONSHIP_ERROR("0024", "Error while activating relationship"),
    SUSPEND_RELATIONSHIP_ERROR("0025", "Error while suspending relationship"),
    PUT_INSTITUTION_ERROR("0051", "Error while updating institution"),
    ONBOARDING_SUBDELEGATES_ERROR("0019", "Error while onboarding subdelegates"),
    ONBOARDING_OPERATORS_ERROR("0020", "Error while onboarding operators"),
    ONBOARDING_LEGALS_ERROR("0018", "Error while onboarding legals"),
    RETRIEVE_GEO_TAXONOMIES_ERROR("0050", "Error while retrieving institution geographic taxonomy"),
    GET_RELATIONSHIP_ERROR("0028", "Error while getting relationship"),
    CREATE_PERSON_ERROR("0009", "Error while creating person"),
    GET_INSTITUTION_ATTRIBUTES_ERROR("0022", "Error while getting party attributes"),

    ONBOARDING_EXPIRED("0040", "Onboarding with id %s not found or it is expired!"),
    ONBOARDING_NOT_TO_BE_VALIDATED("0043", "Onboarding with id %s has not TO_BE_VALIDATED status!"),
    ONBOARDING_NOT_COMPLETED("0044", "Onboarding with id %s must be in COMPLETED status but it is in %s status!"),
    GET_INSTITUTION_BY_GEOTAXONOMY_ERROR("0053", "Error while searching institutions related to given geoTaxonomies"),
    GET_INSTITUTION_BY_PRODUCTID_ERROR("0053", "Error while searching institutions related to given productId"),
    GET_INSTITUTIONS_REQUEST_ERROR("0054", "Invalid request parameters sent. Allowed filters combinations taxCode and subunit or origin and originId"),

    VERIFY_USER_ERROR("0000", "Error while searching institutions related to given productId"),
    VALIDATION_USER_BY_TAXCODE("0000", "Different emails cannot exist for the same tax code (MANAGER/DELEGATE)"),
    GET_USER_ERROR("0000", "Error while searching user given UserID"),
    ATTACHMENT_UPLOAD_ERROR("0000", "Attachment already uploaded"),

    GENERIC_ERROR("0000", "Generic Error");


    private final String code;
    private final String message;

    private static class MessageValue {
        public static final String ERROR_WHILE_RETRIEVING_INSTITUTION_HAVING_EXTERNAL_ID = "Error while retrieving institution having externalId %s";
    }

}
