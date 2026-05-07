package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.UserInstitutionApiRestClientConfig;
import it.pagopa.selfcare.user.generated.openapi.v1.api.InstitutionApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.user-ms.institution.serviceCode}", url = "${rest-client.user-ms.base-url}", configuration = UserInstitutionApiRestClientConfig.class)
public interface UserInstitutionApiRestClient extends InstitutionApi {
}
