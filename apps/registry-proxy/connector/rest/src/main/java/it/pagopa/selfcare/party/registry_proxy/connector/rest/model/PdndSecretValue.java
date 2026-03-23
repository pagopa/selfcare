package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PdndSecretValue {
    private JwtConfig jwtConfig;
    private String clientId;
    private String secretKey;
}
