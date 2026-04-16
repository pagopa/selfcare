package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PDNDConfig {
    @Value("${pdnd.skipLocalizzazioneNodes:false}")
    private Boolean skipLocalizzazioneNodes;

    public Boolean getSkipLocalizzazioneNodes() {
        return skipLocalizzazioneNodes;
    }
}