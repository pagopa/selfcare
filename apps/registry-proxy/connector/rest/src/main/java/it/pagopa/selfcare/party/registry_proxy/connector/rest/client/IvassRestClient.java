package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@PropertySource("classpath:config/ivass-rest-config.properties")
public class IvassRestClient {
    private final RestTemplate ivassRestTemplate;
    private final String ivassBasePath;
    private final String getInsurancesPath;

    public IvassRestClient(
            @Value("${ivass.rest.endpoint}") String ivassBasePath,
            @Value("${ivass.rest.getInsurances.path}") String getInsurancesPath,
            RestTemplate ivassRestTemplate) {
        this.ivassRestTemplate = ivassRestTemplate;
        this.ivassBasePath = ivassBasePath;
        this.getInsurancesPath = getInsurancesPath;
    }

    public byte[] getInsurancesZip() {
        log.info("getInsurances start");
        String apiPath = this.ivassBasePath + getInsurancesPath;
        return ivassRestTemplate.getForObject(apiPath, byte[].class);
    }
}
