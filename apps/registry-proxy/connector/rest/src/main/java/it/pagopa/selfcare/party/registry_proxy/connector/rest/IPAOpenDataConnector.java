package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IPAOpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataAOO;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataCategory;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataUO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(1)
class IPAOpenDataConnector extends OpenDataConnectorTemplate {

    public IPAOpenDataConnector(IPAOpenDataRestClient restClient) {
        super(restClient);
        log.trace("Initializing {}", IPAOpenDataConnector.class.getSimpleName());
    }

    @Override
    protected Class<IPAOpenDataInstitution> getInstitutionType() {
        return IPAOpenDataInstitution.class;
    }

    @Override
    protected Class<IPAOpenDataCategory> getCategoryType() {
        return IPAOpenDataCategory.class;
    }

    @Override
    protected Class<IPAOpenDataAOO> getAOOType() {
        return IPAOpenDataAOO.class;
    }

    @Override
    protected Class<IPAOpenDataUO> getUOType() {
        return IPAOpenDataUO.class;
    }

}
