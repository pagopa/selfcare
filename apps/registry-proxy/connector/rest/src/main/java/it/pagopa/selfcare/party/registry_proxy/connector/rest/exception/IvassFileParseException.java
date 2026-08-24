package it.pagopa.selfcare.party.registry_proxy.connector.rest.exception;

/**
 * Exception thrown when the IVASS CSV file cannot be parsed due to a non-compliant format
 * (e.g., wrong number of columns, unexpected structure). In this case the system should
 * fall back to the last valid file available in Azure Blob Storage.
 */
public class IvassFileParseException extends RuntimeException {

    public IvassFileParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

