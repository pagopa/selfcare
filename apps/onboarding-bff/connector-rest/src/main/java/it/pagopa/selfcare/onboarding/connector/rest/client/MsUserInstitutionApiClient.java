package it.pagopa.selfcare.onboarding.connector.rest.client;


import it.pagopa.selfcare.user.generated.openapi.v1.api.InstitutionControllerApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "${rest-client.ms-user-api-institution.serviceCode}",
    url = "${rest-client.ms-user-institution.base-url}")
public interface MsUserInstitutionApiClient extends InstitutionControllerApi {}
