package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.OpenDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IPAOpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IPAOpenDataConnectorTest extends OpenDataConnectorTemplateTest {

    @Mock
    private IPAOpenDataRestClient restClientMock;

    @InjectMocks
    private IPAOpenDataConnector openDataConnector;


    @Override
    protected OpenDataConnector getOpenDataConnector() {
        return openDataConnector;
    }

    @Override
    protected OpenDataRestClient getOpenDataRestClientMock() {
        return restClientMock;
    }

}