package it.pagopa.selfcare.party.connector.dapr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@PropertySource("classpath:config/dapr-config.properties")
public class DaprConnectorImpl {


}
