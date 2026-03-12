package it.pagopa.selfcare.document.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorMessage {

    ORIGINAL_DOCUMENT_NOT_FOUND("002-1008", "Original document information not found"),
    ATTACHMENT_UPLOAD_ERROR("0000", "Attachment already uploaded"),

    GENERIC_ERROR("0000", "Generic Error");

    private final String code;
    private final String message;

}
