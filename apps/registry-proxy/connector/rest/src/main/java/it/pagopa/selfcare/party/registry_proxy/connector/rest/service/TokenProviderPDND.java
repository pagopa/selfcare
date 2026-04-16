package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.PdndConnectorImpl;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("tokenProviderPDND")
public class TokenProviderPDND implements TokenProvider {

    private final String clientAssertionType;
    private final String grantType;
    private final AssertionGenerator assertionGenerator;
    private final PdndConnectorImpl pdndClient;

    public TokenProviderPDND(AssertionGenerator assertionGenerator,
                         PdndConnectorImpl pdndClient,
                         @Value("${rest-client.pdnd.client-assertion-type}") String clientAssertionType,
                         @Value("${rest-client.pdnd.grant-type}") String grantType) {
        this.assertionGenerator = assertionGenerator;
        this.clientAssertionType = clientAssertionType;
        this.grantType = grantType;
        this.pdndClient = pdndClient;
    }

    @Override
    public ClientCredentialsResponse getTokenPdnd(PdndSecretValue pdndSecretValue) {
        log.info("START - TokenProvider.getTokenPdnd");
        String clientAssertion = assertionGenerator.generateClientAssertion(pdndSecretValue.getJwtConfig(), pdndSecretValue.getSecretKey());
        return pdndClient.createToken(clientAssertion, clientAssertionType, grantType, pdndSecretValue.getClientId());
    }

}
