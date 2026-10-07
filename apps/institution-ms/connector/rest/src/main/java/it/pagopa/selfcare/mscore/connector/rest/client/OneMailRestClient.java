package it.pagopa.selfcare.mscore.connector.rest.client;

import it.pagopa.selfcare.mscore.connector.rest.config.OneMailRestClientConfig;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.api.EmailsApi;
import org.springframework.cloud.openfeign.FeignClient;
@FeignClient(name = "one-mail", url = "${rest-client.one-mail.base-url}", configuration = OneMailRestClientConfig.class)
public interface OneMailRestClient extends EmailsApi {
}
