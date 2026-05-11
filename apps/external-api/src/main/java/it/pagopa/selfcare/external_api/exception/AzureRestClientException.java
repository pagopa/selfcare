package it.pagopa.selfcare.external_api.exception;

public class AzureRestClientException extends RuntimeException{

    private final String code;

    public AzureRestClientException(String message, String code){
        super(message);
        this.code = code;
    }
}
