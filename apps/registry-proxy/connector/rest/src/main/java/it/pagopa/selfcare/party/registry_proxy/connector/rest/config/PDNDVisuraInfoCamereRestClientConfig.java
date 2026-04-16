package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDVisuraInfoCamereRawRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDVisuraInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.JwtConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(RestClientBaseConfig.class)
@EnableFeignClients(clients = { PDNDVisuraInfoCamereRestClient.class, PDNDVisuraInfoCamereRawRestClient.class })
@PropertySource("classpath:config/pdnd-visura-infocamere-rest-client.properties")
public class PDNDVisuraInfoCamereRestClientConfig {
    private final PdndSecretValue pdndSecretValue;

    public PDNDVisuraInfoCamereRestClientConfig(
            @Value("${rest-client.pdnd-visura-infocamere.privateKey}") String privateKey,
            @Value("${rest-client.pdnd-visura-infocamere.clientId}") String clientId,
            @Value("${rest-client.pdnd-visura-infocamere.kid}") String kid,
            @Value("${rest-client.pdnd-visura-infocamere.audience}") String audience,
            @Value("${rest-client.pdnd-visura-infocamere.purposeId}") String purposeId
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
                .build();
    }

    public PdndSecretValue getPdndSecretValue() {
        return pdndSecretValue;
    }
}
