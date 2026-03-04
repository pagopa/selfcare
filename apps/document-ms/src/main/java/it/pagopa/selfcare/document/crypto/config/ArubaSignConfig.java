package it.pagopa.selfcare.document.crypto.config;


import it.pagopa.selfcare.document.crypto.soap.aruba.sign.generated.client.Auth;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArubaSignConfig {
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer requestTimeoutMs;
    private Auth auth;

}
