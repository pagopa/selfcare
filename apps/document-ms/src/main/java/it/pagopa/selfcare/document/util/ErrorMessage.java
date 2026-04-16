package it.pagopa.selfcare.document.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorMessage {

    ORIGINAL_DOCUMENT_NOT_FOUND("002-1008", "Original document information not found"),
    ATTACHMENT_UPLOAD_ERROR("0000", "Attachment already uploaded"),

    DOCUMENT_VALIDATION_FAIL("002-1000", "Error trying to validate document, due: %s"),
    INVALID_CONTRACT_DIGEST("002-1001", "Invalid file digest"),
    SIGNATURE_VALIDATION_ERROR("002-1004", "The tax code related to signature does not match anyone contained in the relationships"),
    INVALID_SIGNATURE_FORMS("002-1003", "Only CAdES signature form is admitted. Invalid signatures forms detected: %s"),
    INVALID_DOCUMENT_SIGNATURE("002-1002", "Document signature is invalid"),
    TAX_CODE_NOT_FOUND_IN_SIGNATURE("002-1006", "No tax code has been found in digital signature"),
    INVALID_SIGNATURE_TAX_CODE("002-1004", "The tax code related to signature does not match anyone contained in the relationships"),
    SIGNATURE_NOT_FOUND("002-1007", "No signature found"),

    GENERIC_ERROR("0000", "Generic Error");

    private final String code;
    private final String message;

}
