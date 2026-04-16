package it.pagopa.selfcare.party.connector.dapr.client;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Slf4j
@PropertySource("classpath:config/dapr-config.properties")
public class DaprSelcClient {


    private final String clientId;
    private final String httpPort;
    private final String grpcPort;
    private final DaprClient daprClient;

    public DaprSelcClient(@Value("${dapr.azureClientId}") String clientId,
                          @Value("${dapr.daprHttpPort}") String httpPort,
                          @Value("${dapr.daprGrpcPort}") String grpcPort) {

        this.clientId = clientId;
        this.httpPort = httpPort;
        this.grpcPort = grpcPort;
        this.daprClient = new DaprClientBuilder().build();
    }

    @Bean
    public DaprClient daprClient() {
        return this.daprClient;
    }
}