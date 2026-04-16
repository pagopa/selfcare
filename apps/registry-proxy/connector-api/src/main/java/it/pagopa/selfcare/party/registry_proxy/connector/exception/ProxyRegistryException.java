package it.pagopa.selfcare.party.registry_proxy.connector.exception;

public class ProxyRegistryException extends RuntimeException {

    private final String code;

    public ProxyRegistryException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
