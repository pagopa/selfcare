package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.JwtConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndProfile;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDNDInvitaliaRestClientConfig {

    private final PdndSecretValue pdndSecretValue;

    public PDNDInvitaliaRestClientConfig(
            @Value("${rest-client.pdnd-invitalia-infocamere.privateKey}") String privateKey,
            @Value("${rest-client.pdnd-invitalia-infocamere.clientId}") String clientId,
            @Value("${rest-client.pdnd-invitalia-infocamere.kid}") String kid,
            @Value("${rest-client.pdnd-invitalia-infocamere.audience}") String audience,
            @Value("${rest-client.pdnd-invitalia-infocamere.purposeId}") String purposeId
    ) {
        JwtConfig jwtConfig = JwtConfig.builder()
                .audience(audience)
                .issuer(clientId)
                .subject(clientId)
                .purposeId(purposeId)
                .kid(kid)
                .build();
        this.pdndSecretValue = PdndSecretValue.builder()
                .clientId(clientId)
                .secretKey(privateKey)
                .jwtConfig(jwtConfig)
                .profile(PdndProfile.INVITALIA)
                .build();
    }

    public PdndSecretValue getPdndSecretValue() {
        return pdndSecretValue;
    }
}

