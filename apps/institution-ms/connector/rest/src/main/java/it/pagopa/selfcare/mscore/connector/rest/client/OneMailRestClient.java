package it.pagopa.selfcare.mscore.connector.rest.client;

import it.pagopa.selfcare.mscore.connector.rest.config.OneMailRestClientConfig;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.api.EmailsApi;
import org.springframework.cloud.openfeign.FeignClient;

// literal name: a placeholder here is not resolved when registering the client configuration (spring-cloud-openfeign 4.1),
// so OneMailRestClientConfig (and the x-api-key interceptor) would not be applied
@FeignClient(name = "one-mail", url = "${rest-client.one-mail.base-url}", configuration = OneMailRestClientConfig.class)
public interface OneMailRestClient extends EmailsApi {
}
