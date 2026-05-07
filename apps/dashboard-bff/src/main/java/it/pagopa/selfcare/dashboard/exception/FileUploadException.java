package it.pagopa.selfcare.dashboard.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FileUploadException extends Exception {

    public FileUploadException(Throwable cause) {
        super(cause);
    }
}
