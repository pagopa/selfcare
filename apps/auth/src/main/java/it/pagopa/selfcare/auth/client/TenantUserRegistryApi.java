package it.pagopa.selfcare.auth.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;

@RegisterRestClient(configKey = "tenant.user-registry")
@RegisterProvider(TenantUserRegistryApiKeyFilter.class)
public interface TenantUserRegistryApi extends UserApi {}
