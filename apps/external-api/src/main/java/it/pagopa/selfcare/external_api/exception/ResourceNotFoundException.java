package it.pagopa.selfcare.external_api.exception;

public class ResourceNotFoundException extends RuntimeException {

    private String code;
    public ResourceNotFoundException(){
    }
    public ResourceNotFoundException(String message){
        super(message);
    }
    public ResourceNotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }
}
