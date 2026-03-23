package it.pagopa.selfcare.party.registry_proxy.connector.exception;

public class InternalException extends RuntimeException{

    public InternalException(Throwable cause) {
        super(cause);
    }

    public InternalException(){
        super();
    }

    public InternalException(String message){
        super(message);
    }

}
