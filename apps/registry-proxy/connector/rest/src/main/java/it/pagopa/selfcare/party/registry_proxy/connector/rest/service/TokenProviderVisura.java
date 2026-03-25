package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.PdndVisuraConnectorImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("tokenProviderVisura")
public class TokenProviderVisura implements TokenProvider {

    private final String clientAssertionType;
    private final String grantType;
    private final AssertionGeneratorVisura assertionGeneratorVisura;
    private final PdndVisuraConnectorImpl pdndVisuraClient;

    public TokenProviderVisura(AssertionGeneratorVisura assertionGeneratorVisura,
                               PdndVisuraConnectorImpl pdndVisuraClient,
                               @Value("${rest-client.pdnd.client-assertion-type}") String clientAssertionType,
                               @Value("${rest-client.pdnd.grant-type}") String grantType) {
        this.assertionGeneratorVisura = assertionGeneratorVisura;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;
        this.pdndVisuraClient = pdndVisuraClient;
    }

    @Override
    public ClientCredentialsResponse getTokenPdnd(PdndSecretValue pdndSecretValue) {
        log.info("START - TokenProviderVisura.getTokenPdnd");
        String clientAssertion = assertionGeneratorVisura.generateClientAssertion(pdndSecretValue.getJwtConfig(), pdndSecretValue.getSecretKey());
        return pdndVisuraClient.createToken(clientAssertion, clientAssertionType, grantType, pdndSecretValue.getClientId());
    }

}
