package it.pagopa.selfcare.dashboard.connector.api;

import it.pagopa.selfcare.dashboard.exception.FileUploadException;

import java.io.InputStream;

public interface FileStorageConnector {

    void uploadInstitutionLogo(InputStream file, String fileName, String contentType) throws FileUploadException;

}
