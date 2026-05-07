package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.UserGroupRestClientConfig;
import it.pagopa.selfcare.group.generated.openapi.v1.api.UserGroupApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.user-groups.serviceCode}", url = "${rest-client.user-groups.base-url}", configuration = UserGroupRestClientConfig.class)
public interface UserGroupRestClient extends UserGroupApi {
}
